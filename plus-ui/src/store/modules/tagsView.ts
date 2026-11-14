import type { LocationQuery, RouteLocationNormalized, RouteMeta } from 'vue-router';
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { useSettingsStore } from './settings';

const PERSIST_KEY = 'tags-view-visited';

type PersistedTagView = {
  path: string;
  fullPath?: string;
  name?: string | symbol | null;
  title?: string;
  query?: LocationQuery;
  meta?: RouteMeta;
};

type TagView = RouteLocationNormalized & {
  title?: string;
};

const isPersistEnabled = () => {
  return useSettingsStore().tagsViewPersist;
};

const normalizeVisitedView = (view: RouteLocationNormalized): TagView => {
  return Object.assign({}, view, {
    title: view.meta?.title || 'no-name'
  });
};

const saveVisitedViews = (views: any[]) => {
  if (!isPersistEnabled()) {
    return;
  }

  const payload: PersistedTagView[] = (views as TagView[])
    .filter(view => !view.meta?.affix)
    .map(view => ({
      path: view.path,
      fullPath: view.fullPath,
      name: view.name,
      title: (view as any).title || view.meta?.title || 'no-name',
      query: view.query,
      meta: view.meta
    }));

  localStorage.setItem(PERSIST_KEY, JSON.stringify(payload));
};

const loadVisitedViews = (): PersistedTagView[] => {
  const cache = localStorage.getItem(PERSIST_KEY);
  if (!cache) {
    return [];
  }

  try {
    return JSON.parse(cache) as PersistedTagView[];
  } catch {
    localStorage.removeItem(PERSIST_KEY);
    return [];
  }
};

const clearVisitedViews = () => {
  localStorage.removeItem(PERSIST_KEY);
};

export const useTagsViewStore = defineStore('tagsView', () => {
  const visitedViews = ref<TagView[]>([]);
  const cachedViews = ref<string[]>([]);
  const iframeViews = ref<TagView[]>([]);

  const getVisitedViews = (): TagView[] => {
    return visitedViews.value as TagView[];
  };
  const getIframeViews = (): TagView[] => {
    return iframeViews.value as TagView[];
  };
  const getCachedViews = (): string[] => {
    return cachedViews.value;
  };

  const addView = (view: RouteLocationNormalized) => {
    addVisitedView(view);
    addCachedView(view);
  };

  const addIframeView = (view: RouteLocationNormalized): void => {
    if (iframeViews.value.some((v: RouteLocationNormalized) => v.path === view.path)) return;
    iframeViews.value.push(normalizeVisitedView(view) as RouteLocationNormalized);
  };

  const delIframeView = (view: RouteLocationNormalized): Promise<RouteLocationNormalized[]> => {
    return new Promise(resolve => {
      iframeViews.value = iframeViews.value.filter((item: RouteLocationNormalized) => item.path !== view.path);
      resolve(iframeViews.value.slice() as RouteLocationNormalized[]);
    });
  };

  const addVisitedView = (view: RouteLocationNormalized): void => {
    if (visitedViews.value.some((v: RouteLocationNormalized) => v.path === view.path)) return;
    visitedViews.value.push(normalizeVisitedView(view));
    saveVisitedViews(visitedViews.value);
  };

  const addAffixView = (view: RouteLocationNormalized): void => {
    if (visitedViews.value.some((v: RouteLocationNormalized) => v.path === view.path)) return;
    const insertIndex = visitedViews.value.findIndex(item => !item.meta?.affix);
    const normalizedView = normalizeVisitedView(view);
    if (insertIndex === -1) {
      visitedViews.value.push(normalizedView);
    } else {
      visitedViews.value.splice(insertIndex, 0, normalizedView);
    }
  };

  const loadPersistedViews = (): void => {
    loadVisitedViews().forEach(view => {
      if (visitedViews.value.some(item => item.path === view.path)) {
        return;
      }

      visitedViews.value.push({
        hash: '',
        matched: [],
        params: {},
        redirectedFrom: undefined,
        path: view.path,
        fullPath: view.fullPath || view.path,
        query: view.query || {},
        name: view.name || undefined,
        meta: view.meta || {},
        title: view.title || view.meta?.title || 'no-name'
      } as TagView);
    });
  };

  const delView = (
    view: RouteLocationNormalized
  ): Promise<{
    visitedViews: RouteLocationNormalized[];
    cachedViews: string[];
  }> => {
    return new Promise(resolve => {
      delVisitedView(view);
      delCachedView(view);
      resolve({
        visitedViews: visitedViews.value.slice() as RouteLocationNormalized[],
        cachedViews: [...cachedViews.value]
      });
    });
  };

  const delVisitedView = (view: RouteLocationNormalized): Promise<RouteLocationNormalized[]> => {
    return new Promise(resolve => {
      for (const [i, v] of visitedViews.value.entries()) {
        if (v.path === view.path) {
          visitedViews.value.splice(i, 1);
          break;
        }
      }
      saveVisitedViews(visitedViews.value);
      resolve(visitedViews.value.slice() as RouteLocationNormalized[]);
    });
  };

  const delCachedView = (view?: RouteLocationNormalized): Promise<string[]> => {
    let viewName = '';
    if (view) {
      viewName = view.name as string;
    }
    return new Promise(resolve => {
      const index = cachedViews.value.indexOf(viewName);
      index > -1 && cachedViews.value.splice(index, 1);
      resolve([...cachedViews.value]);
    });
  };

  const delOthersViews = (
    view: RouteLocationNormalized
  ): Promise<{
    visitedViews: RouteLocationNormalized[];
    cachedViews: string[];
  }> => {
    return new Promise(resolve => {
      delOthersVisitedViews(view);
      delOthersCachedViews(view);
      resolve({
        visitedViews: visitedViews.value.slice() as RouteLocationNormalized[],
        cachedViews: [...cachedViews.value]
      });
    });
  };

  const delOthersVisitedViews = (view: RouteLocationNormalized): Promise<RouteLocationNormalized[]> => {
    return new Promise(resolve => {
      visitedViews.value = visitedViews.value.filter((v: RouteLocationNormalized) => {
        return v.meta?.affix || v.path === view.path;
      });
      saveVisitedViews(visitedViews.value);
      resolve(visitedViews.value.slice() as RouteLocationNormalized[]);
    });
  };

  const delOthersCachedViews = (view: RouteLocationNormalized): Promise<string[]> => {
    const viewName = view.name as string;
    return new Promise(resolve => {
      const index = cachedViews.value.indexOf(viewName);
      if (index > -1) {
        cachedViews.value = cachedViews.value.slice(index, index + 1);
      } else {
        cachedViews.value = [];
      }
      resolve([...cachedViews.value]);
    });
  };

  const delAllViews = (): Promise<{
    visitedViews: RouteLocationNormalized[];
    cachedViews: string[];
  }> => {
    return new Promise(resolve => {
      delAllVisitedViews();
      delAllCachedViews();
      resolve({
        visitedViews: visitedViews.value.slice() as RouteLocationNormalized[],
        cachedViews: [...cachedViews.value]
      });
    });
  };

  const delAllVisitedViews = (): Promise<RouteLocationNormalized[]> => {
    return new Promise(resolve => {
      visitedViews.value = visitedViews.value.filter((tag: RouteLocationNormalized) => tag.meta?.affix);
      clearVisitedViews();
      resolve(visitedViews.value.slice() as RouteLocationNormalized[]);
    });
  };

  const delAllCachedViews = (): Promise<string[]> => {
    return new Promise(resolve => {
      cachedViews.value = [];
      resolve([...cachedViews.value]);
    });
  };

  const updateVisitedView = (view: RouteLocationNormalized): void => {
    for (let v of visitedViews.value) {
      if (v.path === view.path) {
        v = Object.assign(v, view);
        break;
      }
    }
    saveVisitedViews(visitedViews.value);
  };

  const delRightTags = (view: RouteLocationNormalized): Promise<RouteLocationNormalized[]> => {
    return new Promise(resolve => {
      const index = visitedViews.value.findIndex((v: RouteLocationNormalized) => v.path === view.path);
      if (index === -1) {
        resolve(visitedViews.value.slice() as RouteLocationNormalized[]);
        return;
      }
      visitedViews.value = visitedViews.value.filter((item: RouteLocationNormalized, idx: number) => {
        if (idx <= index || (item.meta && item.meta.affix)) {
          return true;
        }
        const i = cachedViews.value.indexOf(item.name as string);
        if (i > -1) {
          cachedViews.value.splice(i, 1);
        }
        return false;
      });
      saveVisitedViews(visitedViews.value);
      resolve(visitedViews.value.slice() as RouteLocationNormalized[]);
    });
  };

  const delLeftTags = (view: RouteLocationNormalized): Promise<RouteLocationNormalized[]> => {
    return new Promise(resolve => {
      const index = visitedViews.value.findIndex((v: RouteLocationNormalized) => v.path === view.path);
      if (index === -1) {
        resolve(visitedViews.value.slice() as RouteLocationNormalized[]);
        return;
      }
      visitedViews.value = visitedViews.value.filter((item: RouteLocationNormalized, idx: number) => {
        if (idx >= index || (item.meta && item.meta.affix)) {
          return true;
        }
        const i = cachedViews.value.indexOf(item.name as string);
        if (i > -1) {
          cachedViews.value.splice(i, 1);
        }
        return false;
      });
      saveVisitedViews(visitedViews.value);
      resolve(visitedViews.value.slice() as RouteLocationNormalized[]);
    });
  };

  const addCachedView = (view: RouteLocationNormalized): void => {
    const viewName = view.name as string;
    if (!viewName) return;
    if (cachedViews.value.includes(viewName)) return;
    if (!view.meta?.noCache) {
      cachedViews.value.push(viewName);
    }
  };

  return {
    visitedViews,
    cachedViews,
    iframeViews,

    getVisitedViews,
    getIframeViews,
    getCachedViews,

    addVisitedView,
    addAffixView,
    addCachedView,
    delVisitedView,
    delCachedView,
    updateVisitedView,
    addView,
    delView,
    delAllViews,
    delAllVisitedViews,
    delAllCachedViews,
    delOthersViews,
    delRightTags,
    delLeftTags,
    addIframeView,
    delIframeView,
    loadPersistedViews
  };
});
