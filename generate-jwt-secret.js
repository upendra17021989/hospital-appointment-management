#!/usr/bin/env node
// ============================================================
// generate-jwt-secret.js
// Run: node generate-jwt-secret.js
// Generates a secure 64-byte base64 secret for JWT signing
// ============================================================
const crypto = require('crypto');
const secret = crypto.randomBytes(64).toString('base64');
console.log('\n✅ Your JWT Secret (copy this into application.properties):\n');
console.log('app.jwt.secret=' + secret);
console.log('\n⚠️  Keep this secret safe — never commit it to Git!\n');
