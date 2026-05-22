export function normalizeStatus(value?: string | null): string {
  return (value ?? '').toLowerCase()
}

export function isEnrichmentPending(status?: string | null): boolean {
  const normalized = normalizeStatus(status)
  return normalized !== 'done' && normalized !== 'failed'
}
