import { useRoute } from 'vue-router';
import { useSettingsStore } from '@/store/modules/settings';

interface VueBackedHTMLElement extends HTMLElement {
  [key: string]: unknown;
}

interface TableComponentHost {
  proxy?: {
    doLayout?: () => void;
  };
}

interface TableState {
  height: string;
  maxHeight: string;
  bodyHeight: string;
  bodyMaxHeight: string;
  bodyOverflowY: string;
  bodyOverflowX: string;
  appliedHeight?: number;
}

const tableStates = new WeakMap<HTMLElement, TableState>();
const vueParentComponentKey = '__vueParentComponent';

const getTableComponentHost = (table: HTMLElement): TableComponentHost | undefined => {
  const host = (table as VueBackedHTMLElement)[vueParentComponentKey];
  if (!host || typeof host !== 'object') return undefined;
  return host as TableComponentHost;
};

const getOuterHeight = (element: HTMLElement) => {
  const style = window.getComputedStyle(element);
  const marginTop = Number.parseFloat(style.marginTop) || 0;
  const marginBottom = Number.parseFloat(style.marginBottom) || 0;
  return element.getBoundingClientRect().height + marginTop + marginBottom;
};

const isVisible = (element: HTMLElement) => {
  const rect = element.getBoundingClientRect();
  return rect.width > 0 && rect.height > 0;
};

const getFollowingHeight = (table: HTMLElement) => {
  const parent = table.parentElement;
  if (!parent) return 0;

  const children = Array.from(parent.children) as HTMLElement[];
  const tableIndex = children.indexOf(table);
  if (tableIndex === -1) return 0;

  return children.slice(tableIndex + 1).reduce((height, child) => {
    if (!isVisible(child)) return height;
    return height + getOuterHeight(child);
  }, 0);
};

const getBottomPadding = (table: HTMLElement) => {
  const parent = table.parentElement;
  if (!parent) return 0;
  const parentStyle = window.getComputedStyle(parent);
  return Number.parseFloat(parentStyle.paddingBottom) || 0;
};

const getBodyWrapper = (table: HTMLElement) => {
  return table.querySelector<HTMLElement>('.el-table__body-wrapper');
};

const getTableComponent = (table: HTMLElement) => {
  return getTableComponentHost(table)?.proxy;
};

const rememberTableState = (table: HTMLElement) => {
  if (tableStates.has(table)) return;

  const bodyWrapper = getBodyWrapper(table);
  tableStates.set(table, {
    height: table.style.height,
    maxHeight: table.style.maxHeight,
    bodyHeight: bodyWrapper?.style.height ?? '',
    bodyMaxHeight: bodyWrapper?.style.maxHeight ?? '',
    bodyOverflowY: bodyWrapper?.style.overflowY ?? '',
    bodyOverflowX: bodyWrapper?.style.overflowX ?? ''
  });
};

const resetTable = (table: HTMLElement) => {
  const originalState = tableStates.get(table);
  if (!originalState) return;

  const bodyWrapper = getBodyWrapper(table);
  table.classList.remove('is-full-height-table');
  table.style.height = originalState.height;
  table.style.maxHeight = originalState.maxHeight;
  if (bodyWrapper) {
    bodyWrapper.style.height = originalState.bodyHeight;
    bodyWrapper.style.maxHeight = originalState.bodyMaxHeight;
    bodyWrapper.style.overflowY = originalState.bodyOverflowY;
    bodyWrapper.style.overflowX = originalState.bodyOverflowX;
  }
  tableStates.delete(table);
  getTableComponent(table)?.doLayout?.();
};

const resetAllTables = () => {
  document.querySelectorAll<HTMLElement>('.is-full-height-table').forEach(resetTable);
};

const calculateTableHeight = (table: HTMLElement) => {
  let layoutTop = 0;
  let node: HTMLElement | null = table;
  while (node) {
    layoutTop += node.offsetTop;
    node = node.offsetParent as HTMLElement | null;
  }

  // Keep the calculation tied to the page layout. Using the current scroll
  // position here would make scrolling change the table height again.
  const viewportBottom = window.innerHeight;
  const followingHeight = getFollowingHeight(table);
  const bottomPadding = getBottomPadding(table);
  const bottomOffset = window.innerWidth < 768 ? 28 : 32;
  const minHeight = window.innerWidth < 768 ? 220 : 260;

  return Math.max(minHeight, Math.floor(viewportBottom - layoutTop - followingHeight - bottomPadding - bottomOffset));
};

const applyTableHeightValue = (table: HTMLElement, tableHeight: number) => {
  const nextHeight = `${tableHeight}px`;
  if (table.style.height !== nextHeight) {
    table.style.height = nextHeight;
  }
  if (table.style.maxHeight !== nextHeight) {
    table.style.maxHeight = nextHeight;
  }
  table.classList.add('is-full-height-table');

  const bodyWrapper = getBodyWrapper(table);
  if (bodyWrapper) {
    const headerHeight = table.querySelector<HTMLElement>('.el-table__header-wrapper')?.offsetHeight ?? 0;
    const footerHeight = table.querySelector<HTMLElement>('.el-table__footer-wrapper')?.offsetHeight ?? 0;
    const bodyHeight = `${Math.max(120, tableHeight - headerHeight - footerHeight)}px`;
    if (bodyWrapper.style.height !== bodyHeight) {
      bodyWrapper.style.height = bodyHeight;
    }
    if (bodyWrapper.style.maxHeight !== bodyHeight) {
      bodyWrapper.style.maxHeight = bodyHeight;
    }
    bodyWrapper.style.overflowY = 'auto';
    bodyWrapper.style.overflowX = 'auto';
  }

  getTableComponent(table)?.doLayout?.();
};

const applyTableHeight = (table: HTMLElement) => {
  rememberTableState(table);

  const tableHeight = calculateTableHeight(table);
  const state = tableStates.get(table);
  if (state?.appliedHeight === tableHeight) return;
  if (state) state.appliedHeight = tableHeight;
  applyTableHeightValue(table, tableHeight);
};

const canManageTable = (table: HTMLElement) => {
  if (!isVisible(table)) return false;
  if (!table.closest('.app-main')) return false;
  if (!table.closest('.table-panel')) return false;
  if (table.closest('.el-dialog')) return false;
  if (table.dataset.fullHeightTable === 'false') return false;

  const hasManualHeight = Boolean(table.style.height || table.style.maxHeight);
  return !hasManualHeight || tableStates.has(table);
};

const getManagedTables = () => {
  return Array.from(document.querySelectorAll<HTMLElement>('.app-main .table-panel .data-table.el-table')).filter(
    canManageTable
  );
};

export function useFullHeightTable() {
  const route = useRoute();
  const settingsStore = useSettingsStore();
  const observedElements = new Set<Element>();
  let resizeObserver: ResizeObserver | undefined;
  let mutationObserver: MutationObserver | undefined;
  let animationFrame = 0;
  const timerIds = new Set<number>();

  const refreshObservedElements = () => {
    if (!resizeObserver) return;

    const nextElements = new Set<Element>([
      ...Array.from(document.querySelectorAll('.app-main, .app-main .p-2, .app-main .table-panel')),
      ...Array.from(document.querySelectorAll('.app-main .search-panel, .app-main .tree-panel-shell')),
    ]);

    observedElements.forEach(element => {
      if (!nextElements.has(element)) {
        resizeObserver?.unobserve(element);
        observedElements.delete(element);
      }
    });

    nextElements.forEach(element => {
      if (!observedElements.has(element)) {
        resizeObserver?.observe(element);
        observedElements.add(element);
      }
    });
  };

  const recalculate = () => {
    animationFrame = 0;
    if (!settingsStore.fullHeightTable) {
      resetAllTables();
      return;
    }

    const tables = getManagedTables();
    document.querySelectorAll<HTMLElement>('.is-full-height-table').forEach(table => {
      if (!tables.includes(table)) {
        resetTable(table);
      }
    });
    tables.forEach(applyTableHeight);
    refreshObservedElements();
  };

  const scheduleRecalculate = (delay = 0) => {
    if (delay > 0) {
      const timerId = window.setTimeout(() => {
        timerIds.delete(timerId);
        scheduleRecalculate();
      }, delay);
      timerIds.add(timerId);
      return;
    }
    if (animationFrame) return;
    animationFrame = window.requestAnimationFrame(recalculate);
  };

  const scheduleLayoutRecheck = () => {
    scheduleRecalculate();
    [80, 180, 360].forEach(scheduleRecalculate);
  };

  onMounted(() => {
    resizeObserver = new ResizeObserver(() => scheduleLayoutRecheck());
    mutationObserver = new MutationObserver(() => {
      refreshObservedElements();
      scheduleLayoutRecheck();
    });

    const appMain = document.querySelector('.app-main');
    if (appMain) {
      mutationObserver.observe(appMain, {
        attributes: true,
        attributeFilter: ['class', 'style'],
        childList: true,
        subtree: true
      });
      appMain.addEventListener('transitionend', scheduleLayoutRecheck);
    }
    window.addEventListener('resize', scheduleLayoutRecheck);

    refreshObservedElements();
    scheduleLayoutRecheck();
  });

  onBeforeUnmount(() => {
    if (animationFrame) {
      window.cancelAnimationFrame(animationFrame);
    }
    timerIds.forEach(timerId => window.clearTimeout(timerId));
    timerIds.clear();
    resizeObserver?.disconnect();
    mutationObserver?.disconnect();
    document.querySelector('.app-main')?.removeEventListener('transitionend', scheduleLayoutRecheck);
    window.removeEventListener('resize', scheduleLayoutRecheck);
    resetAllTables();
  });

  watch(
    () => settingsStore.fullHeightTable,
    () => scheduleLayoutRecheck()
  );

  watch(
    () => route.fullPath,
    () => {
      nextTick(() => scheduleLayoutRecheck());
    }
  );
}
