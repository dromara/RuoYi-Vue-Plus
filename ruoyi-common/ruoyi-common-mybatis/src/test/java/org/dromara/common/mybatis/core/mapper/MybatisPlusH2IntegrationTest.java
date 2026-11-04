package org.dromara.common.mybatis.core.mapper;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.injector.MPJSqlInjector;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MyBatis-Plus H2 SQL 集成测试")
class MybatisPlusH2IntegrationTest {

    private SqlSessionFactory sqlSessionFactory;

    @BeforeEach
    void setUp() throws Exception {
        String url = "jdbc:h2:mem:mp_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1";
        UnpooledDataSource dataSource = new UnpooledDataSource("org.h2.Driver", url, "sa", "");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE audit_record (id BIGINT PRIMARY KEY, owner_id BIGINT, name VARCHAR(40))");
            statement.execute("CREATE TABLE audit_extra (record_id BIGINT PRIMARY KEY, flag INT)");
            statement.execute("INSERT INTO audit_record VALUES (1, 1, 'one'), (2, 2, 'two'), (3, 1, 'three'), (4, 2, 'four')");
            statement.execute("INSERT INTO audit_extra VALUES (1, 0), (2, 1), (3, 0), (4, 0)");
        }

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("h2", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new MultiDataPermissionHandler() {
            @Override
            public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
                if (!"audit_record".equalsIgnoreCase(table.getName())) {
                    return null;
                }
                String qualifier = table.getAlias() == null ? "audit_record" : table.getAlias().getName();
                try {
                    return CCJSqlParserUtil.parseCondExpression(qualifier + ".owner_id = 1");
                } catch (JSQLParserException e) {
                    throw new IllegalStateException(e);
                }
            }
        }));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        configuration.addInterceptor(interceptor);
        GlobalConfigUtils.setGlobalConfig(configuration, new GlobalConfig()
            .setDbConfig(new GlobalConfig.DbConfig())
            .setSqlInjector(new MPJSqlInjector()));
        configuration.addMapper(AuditMapper.class);
        configuration.addMapper(AuditExtraMapper.class);
        sqlSessionFactory = new MybatisSqlSessionFactoryBuilder().build(configuration);
    }

    @Test
    @DisplayName("联表分页保留权限过滤和总数")
    void shouldPageJoinedQueryWithPermission() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            Page<AuditRecord> page = new Page<>(1, 1);
            List<AuditRecord> records = session.getMapper(AuditMapper.class).findPage(page, 1L);

            assertEquals(2, page.getTotal());
            assertEquals(List.of(1L), records.stream().map(AuditRecord::getId).toList());
        }
    }

    @Test
    @DisplayName("OR 查询和更新均受权限条件约束")
    void shouldRestrictOrSelectAndUpdate() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            AuditMapper mapper = session.getMapper(AuditMapper.class);

            assertEquals(List.of(1L), mapper.findByEitherId(1L, 2L).stream().map(AuditRecord::getId).toList());
            assertEquals(1, mapper.renameEitherId(1L, 2L));
            assertEquals("changed", mapper.selectById(1L).getName());
            session.commit();
            try (Connection connection = sqlSessionFactory.getConfiguration().getEnvironment().getDataSource().getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT name FROM audit_record WHERE id = 2")) {
                assertTrue(result.next());
                assertEquals("two", result.getString(1));
            } catch (Exception e) {
                fail(e);
            }
        }
    }

    @Test
    @DisplayName("selectOne 多行时保持异常和非异常模式")
    void shouldKeepSelectOneSemantics() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            AuditMapper mapper = session.getMapper(AuditMapper.class);
            LambdaQueryWrapper<AuditRecord> query = new LambdaQueryWrapper<AuditRecord>()
                .eq(AuditRecord::getOwnerId, 1L);

            assertThrows(TooManyResultsException.class, () -> mapper.selectOne(query));
            assertEquals(1L, mapper.selectOne(query, false).getId());
        }
    }

    @Test
    @DisplayName("BaseMapperPlus 链式查询使用真实 Mapper SQL")
    void shouldExecuteMapperChain() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            AuditMapper mapper = session.getMapper(AuditMapper.class);

            assertEquals(List.of(1L, 3L), mapper.lambda()
                .eq(AuditRecord::getOwnerId, 1L)
                .orderByAsc(AuditRecord::getId)
                .list().stream().map(AuditRecord::getId).toList());
        }
    }

    @Test
    @DisplayName("PageQuery 分页正确返回记录和总数")
    void shouldExecutePageQuery() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PageQuery pageQuery = new PageQuery(1, 2);
            Page<AuditRecord> page = pageQuery.build();
            List<AuditRecord> records = session.getMapper(AuditMapper.class).selectList(page,
                QueryBuilder.lambda(AuditRecord.class)
                    .eq(AuditRecord::getOwnerId, 1L)
                    .orderByAsc(AuditRecord::getId)
                    .build());

            assertEquals(2, page.getTotal());
            assertEquals(List.of(3L), records.stream().map(AuditRecord::getId).toList());
        }
    }

    @Test
    @DisplayName("BaseMapperPlus 基础写入和删除实际执行")
    void shouldExecuteCrud() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            AuditMapper mapper = session.getMapper(AuditMapper.class);
            AuditRecord record = new AuditRecord();
            record.id = 5L;
            record.ownerId = 1L;
            record.name = "five";

            assertEquals(1, mapper.insert(record));
            assertEquals("five", mapper.selectById(5L).getName());
            record.name = "updated";
            assertEquals(1, mapper.updateById(record));
            assertEquals("updated", mapper.selectById(5L).getName());
            assertEquals(1, mapper.deleteById(5L));
            assertNull(mapper.selectById(5L));
        }
    }

    @Test
    @DisplayName("MPJ 联表分页正确过滤记录和统计总数")
    void shouldExecuteMpjJoinPage() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            AuditMapper mapper = session.getMapper(AuditMapper.class);
            Page<AuditRecord> page = mapper.selectJoinPage(new Page<>(1, 1), AuditRecord.class,
                QueryBuilder.lambdaJoin("r", AuditRecord.class)
                    .selectAll(AuditRecord.class)
                    .leftJoin(AuditExtra.class, "e", AuditExtra::getRecordId, AuditRecord::getId)
                    .eq("e", AuditExtra::getFlag, 0)
                    .build());

            assertEquals(2, page.getTotal());
            assertEquals(1, page.getRecords().size());
            assertEquals(1L, page.getRecords().getFirst().getId());
        }
    }

    public interface AuditMapper extends BaseMapperPlus<AuditRecord, AuditRecord>, MPJBaseMapper<AuditRecord> {

        @Select("SELECT r.id, r.owner_id, r.name FROM audit_record r LEFT JOIN audit_extra e ON e.record_id = r.id " +
            "WHERE r.owner_id = #{ownerId} OR e.flag = 1 ORDER BY r.id")
        List<AuditRecord> findPage(Page<AuditRecord> page, @Param("ownerId") Long ownerId);

        @Select("SELECT id, owner_id, name FROM audit_record WHERE id = #{firstId} OR id = #{secondId} ORDER BY id")
        List<AuditRecord> findByEitherId(@Param("firstId") Long firstId, @Param("secondId") Long secondId);

        @Update("UPDATE audit_record SET name = 'changed' WHERE id = #{firstId} OR id = #{secondId}")
        int renameEitherId(@Param("firstId") Long firstId, @Param("secondId") Long secondId);
    }

    public interface AuditExtraMapper extends BaseMapperPlus<AuditExtra, AuditExtra> {
    }

    @TableName("audit_record")
    public static class AuditRecord {

        @TableId
        private Long id;
        private Long ownerId;
        private String name;

        public Long getId() {
            return id;
        }

        public Long getOwnerId() {
            return ownerId;
        }

        public String getName() {
            return name;
        }
    }

    @TableName("audit_extra")
    public static class AuditExtra {

        @TableId
        private Long recordId;
        private Integer flag;

        public Long getRecordId() {
            return recordId;
        }

        public Integer getFlag() {
            return flag;
        }
    }
}
