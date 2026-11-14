import type { LoadingInstance, MessageBoxData } from 'element-plus';

let loadingInstance: LoadingInstance | undefined;
export default {
  // 消息提示
  msg(content: any) {
    ElMessage.info(content);
  },
  // 错误消息
  msgError(content: any) {
    ElMessage.error(content);
  },
  // 成功消息
  msgSuccess(content: any) {
    ElMessage.success(content);
  },
  // 警告消息
  msgWarning(content: any) {
    ElMessage.warning(content);
  },
  // 弹出提示
  alert(content: any) {
    ElMessageBox.alert(content, '系统提示');
  },
  // 错误提示
  alertError(content: any) {
    ElMessageBox.alert(content, '系统提示', { type: 'error' });
  },
  // 成功提示
  alertSuccess(content: any) {
    ElMessageBox.alert(content, '系统提示', { type: 'success' });
  },
  // 警告提示
  alertWarning(content: any) {
    ElMessageBox.alert(content, '系统提示', { type: 'warning' });
  },
  // 通知提示
  notify(content: any) {
    ElNotification.info(content);
  },
  // 错误通知
  notifyError(content: any) {
    ElNotification.error(content);
  },
  // 成功通知
  notifySuccess(content: any) {
    ElNotification.success(content);
  },
  // 警告通知
  notifyWarning(content: any) {
    ElNotification.warning(content);
  },
  // 确认窗体
  confirm(content: any): Promise<MessageBoxData> {
    return ElMessageBox.confirm(content, '系统提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
  },
  // 提交内容
  prompt(content: any) {
    return ElMessageBox.prompt(content, '系统提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
  },
  // 打开遮罩层
  loading(content: string) {
    loadingInstance = ElLoading.service({
      lock: true,
      text: content,
      background: 'var(--app-overlay-mask)'
    });
  },
  // 关闭遮罩层
  closeLoading() {
    loadingInstance?.close();
  }
};
