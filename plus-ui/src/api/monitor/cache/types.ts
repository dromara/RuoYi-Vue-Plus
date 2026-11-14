export interface CacheVO {
  commandStats: Array<{ name: string; value: string }>;

  dbSize: number;

  info: { [key: string]: string };
}
