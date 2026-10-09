// Shapes of the JSON returned by product-service and inventory-service.

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
}

export interface Product {
  id: number;
  name: string;
  description?: string;
  price: number;
  category?: string;
  skuCode: string;
  createdAt: string;
  updatedAt: string;
  /** Only meaningful on single-item reads. Do not rely on it in lists. */
  stockQuantity?: number;
  // The four fields below are omitted by the server when null.
  brand?: string;
  /** List price before any discount. */
  mrp?: number;
  /** Whole percent, derived by the server. Absent when there is no discount. */
  discountPercent?: number;
  /** The first image of the product. */
  imageUrl?: string;
}

export interface ProductImage {
  url: string;
  alt?: string;
}

export interface SpecItem {
  key: string;
  value: string;
}

/** Already grouped and ordered by the server. */
export interface SpecGroup {
  group: string;
  items: SpecItem[];
}

/** `GET /api/products/{id}/details`: the product with everything the detail page needs. */
export interface ProductDetail {
  id: number;
  name: string;
  description?: string;
  price: number;
  mrp?: number;
  discountPercent?: number;
  category?: string;
  brand?: string;
  skuCode: string;
  warranty?: string;
  seller?: string;
  /** Absent when the inventory service is unreachable or has no record. */
  stockQuantity?: number | null;
  images: ProductImage[];
  highlights: string[];
  specifications: SpecGroup[];
  createdAt: string;
  updatedAt: string;
}

export interface InventoryItem {
  id: number;
  skuCode: string;
  quantityOnHand: number;
  reorderThreshold: number;
  createdAt: string;
  updatedAt: string;
}

export type ChangeType = 'CREATED' | 'ADJUSTED' | 'UPDATED' | 'DELETED';

export interface AuditEntry {
  id: number;
  skuCode: string;
  changeType: ChangeType;
  previousQuantity?: number;
  newQuantity?: number;
  changedAt: string;
}

/** skuCode -> quantity. Unknown SKUs are absent. */
export type StockMap = Record<string, number>;

export interface ServerInfo {
  application: {
    name: string;
    version: string;
    profiles: string[];
    port: number;
  };
  host: {
    hostname: string;
    addresses: { interfaceName: string; address: string }[];
    container: boolean;
  };
  request: {
    hostHeader?: string;
    serverAddress: string;
    serverPort: number;
    clientAddress: string;
    forwardedFor?: string;
  };
  runtime: {
    javaVersion: string;
    javaVendor: string;
    osName: string;
    osVersion: string;
    osArch: string;
    cpus: number;
    heapUsedMb: number;
    heapMaxMb: number;
    heapUsedPercent: number;
    pid: number;
    startedAt: string;
    startedAtEpochMs: number;
    uptime: string;
  };
  cloud?: {
    instanceId?: string;
    instanceType?: string;
    availabilityZone?: string;
    region?: string;
    privateIp?: string;
    publicIp?: string;
    amiId?: string;
  };
}
