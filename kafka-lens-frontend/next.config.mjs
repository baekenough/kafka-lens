/** @type {import('next').NextConfig} */
const nextConfig = {
  // Static export for Tauri embedding
  output: 'export',
  trailingSlash: true,

  // Strict mode
  reactStrictMode: true,

  // Image optimization disabled for static export
  images: {
    unoptimized: true,
  },

  // Environment variables
  env: {
    NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080',
  },

  // API proxy for development mode
  async rewrites() {
    // rewrites are not supported in static export
    // This is only used during `next dev`
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/:path*',
      },
    ];
  },
};

export default nextConfig;
