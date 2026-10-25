/**
 * 触发浏览器下载二进制内容。
 *
 * @param blob 文件内容
 * @param filename 保存的文件名
 */
export function saveBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/**
 * 解析服务端返回的导出文件名，取自 Content-Disposition。
 */
export function filenameFromHeader(header: string | undefined, fallback: string) {
  if (!header) return fallback
  const match = /filename\*=UTF-8''([^;]+)/i.exec(header) || /filename="?([^";]+)"?/i.exec(header)
  if (!match) return fallback
  try {
    return decodeURIComponent(match[1])
  } catch {
    return match[1]
  }
}
