// Shared frontend utilities
// This file exists to support imports like: import { API_BASE } from './utils';

export const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

