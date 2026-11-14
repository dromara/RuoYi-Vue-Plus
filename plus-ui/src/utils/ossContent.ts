import { listByIds } from '@/api/system/oss';

const OSS_MARKER_RE = /oss:\/\/([\w-]+)/g;

/**
 * 将 HTML 中的 oss://{ossId} 标记批量解析为真实的 OSS 授权 URL
 *
 * 适用于富文本内容展示场景（Editor 组件 / 详情页只读渲染等）
 *
 * @example
 * const html = await resolveOssContent('<p><img src="oss://12345"/></p>');
 */
export async function resolveOssContent(html: string): Promise<string> {
  if (!html) return html;

  const matches = [...html.matchAll(OSS_MARKER_RE)];
  if (matches.length === 0) return html;

  const ossIds = [...new Set(matches.map(m => m[1]))];

  try {
    const res = await listByIds(ossIds.join(','));
    let result = html;
    for (const oss of res.data) {
      result = result.replaceAll(`oss://${oss.ossId}`, oss.url);
    }
    return result;
  } catch {
    return html;
  }
}
