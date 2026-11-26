/**
 * Adapter contract reserved for future coding platform integrations (e.g., LeetCode).
 * Site observation and DOM extraction are intentionally NOT implemented in Task 1.
 */
export interface PlatformAdapter {
  readonly platformId: string;
  isSupportedUrl(url: string): boolean;
}
