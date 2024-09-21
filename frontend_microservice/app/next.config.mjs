const nextConfig = {
  assetPrefix: process.env.NODE_ENV === 'production' ? 'http://localhost:8080' : '',
};

export default nextConfig;
