import { LocationQuery, type RouteMeta as VRouteMeta } from 'vue-router';
declare module 'vue-router' {
  interface RouteMeta extends VRouteMeta {
    link?: string;
    title?: string;
    affix?: boolean;
    noCache?: boolean;
    activeMenu?: string;
    icon?: string;
    breadcrumb?: boolean;
  }

  interface _RouteRecordBase {
    hidden?: boolean | string | number;
    permissions?: string[];
    roles?: string[];
    alwaysShow?: boolean;
    query?: string;
    ext?: string;
    parentPath?: string;
  }

  interface _RouteLocationBase {
    children?: _RouteRecordBase[];
    path?: string;
    title?: string;
  }

  interface TagView {
    fullPath?: string;
    name?: string;
    path?: string;
    title?: string;
    meta?: RouteMeta;
    query?: LocationQuery;
  }
}
