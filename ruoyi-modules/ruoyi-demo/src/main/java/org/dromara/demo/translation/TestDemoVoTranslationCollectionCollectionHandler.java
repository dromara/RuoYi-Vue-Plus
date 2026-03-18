package org.dromara.demo.translation;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.translation.collection.TranslationCollectionType;
import org.dromara.common.translation.collection.TranslationCollectionProcessor;
import org.dromara.demo.domain.vo.TestDemoVo;

import java.util.ArrayList;
import java.util.Collection;

@Slf4j
@TranslationCollectionType(type = "test")
public class TestDemoVoTranslationCollectionCollectionHandler implements TranslationCollectionProcessor<TestDemoVo,TestDemoVo> {

    @Override
    public Collection<TestDemoVo> process(Collection<TestDemoVo> value, String other) throws Exception {
        log.info("翻译前的数据：{}", JsonUtils.toJsonString(value));
        ArrayList<TestDemoVo> vos = new ArrayList<>(value);
        for (TestDemoVo vo : vos) {
            vo.setValue("啊啦啦啦啦啦啦啦啦啦:"+ IdUtil.fastSimpleUUID());
        }
        log.info("翻译后的数据：{}", JsonUtils.toJsonString(vos));
        return vos;
    }
}
