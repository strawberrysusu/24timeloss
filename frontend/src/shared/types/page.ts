export interface PageResponse<T> {
  content: T[];
  last: boolean;
  number: number;
  size: number;
  totalPages: number;
  totalElements: number;
}
