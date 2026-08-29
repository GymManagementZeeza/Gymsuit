import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // React Strict Mode's dev-only double-invoke of effects tears down and
  // recreates the WebGL context right after the /scanner scene mounts,
  // which looks like the scene rendering then disappearing.
  reactStrictMode: false,
};

export default nextConfig;
