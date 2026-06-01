package org.dromara.system.domain.vo;

import lombok.Data;
import org.dromara.common.core.utils.StringUtils;

/**
 * 路由显示信息
 *
 * @author ruoyi
 */

@Data
public class MetaVo {

    /**
     * 设置该路由在侧边栏和面包屑中展示的名字
     */
    private String title;

    /**
     * 设置该路由的图标，对应路径src/assets/icons/svg
     */
    private String icon;

    /**
     * 设置为true，则不会被 <keep-alive>缓存
     */
    private Boolean noCache;

    /**
     * 内链地址（http(s)://开头）
     */
    private String link;

    /**
     * 激活菜单
     */
    private String activeMenu;

    /**
     * 构造路由显示信息。
     *
     * @param title 路由标题
     * @param icon  路由图标
     */
    public MetaVo(String title, String icon) {
        this.title = title;
        this.icon = icon;
    }

    /**
     * 构造路由显示信息。
     *
     * @param title   路由标题
     * @param icon    路由图标
     * @param noCache 是否不缓存
     */
    public MetaVo(String title, String icon, Boolean noCache) {
        this.title = title;
        this.icon = icon;
        this.noCache = noCache;
    }

    /**
     * 构造带内链地址的路由显示信息。
     *
     * @param title 路由标题
     * @param icon  路由图标
     * @param link  内链地址
     */
    public MetaVo(String title, String icon, String link) {
        this.title = title;
        this.icon = icon;
        this.link = link;
    }

    /**
     * 构造带缓存配置和内链地址的路由显示信息。
     *
     * @param title   路由标题
     * @param icon    路由图标
     * @param noCache 是否不缓存
     * @param link    内链地址
     */
    public MetaVo(String title, String icon, Boolean noCache, String link) {
        this.title = title;
        this.icon = icon;
        this.noCache = noCache;
        if (StringUtils.ishttp(link)) {
            this.link = link;
        }
    }

    /**
     * 构造带激活菜单的路由显示信息。
     *
     * @param title      路由标题
     * @param icon       路由图标
     * @param noCache    是否不缓存
     * @param link       内链地址
     * @param activeMenu 激活菜单路径
     */
    public MetaVo(String title, String icon, Boolean noCache, String link, String activeMenu) {
        this.title = title;
        this.icon = icon;
        this.noCache = noCache;
        if (StringUtils.ishttp(link)) {
            this.link = link;
        }
        if (StringUtils.startWithAnyIgnoreCase(activeMenu, "/")) {
            this.activeMenu = activeMenu;
        }
    }

}
