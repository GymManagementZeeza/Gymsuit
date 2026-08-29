"use client";

import { extend } from "@react-three/fiber";
import { shaderMaterial } from "@react-three/drei";
import * as THREE from "three";

/**
 * uState: 0 = idle (dim amber standby), 1 = hover (blue pulse),
 * 2 = scanning (green swipe), 3 = success (solid green)
 */
const ScanPlateMaterialImpl = shaderMaterial(
  {
    uTime: 0,
    uState: 0,
    uProgress: 0,
  },
  /* vertex */ `
    varying vec2 vUv;
    void main() {
      vUv = uv;
      gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
    }
  `,
  /* fragment */ `
    uniform float uTime;
    uniform float uState;
    uniform float uProgress;
    varying vec2 vUv;

    vec3 idleColor = vec3(0.35, 0.22, 0.05);
    vec3 hoverColor = vec3(0.0, 0.69, 1.0);
    vec3 scanColor = vec3(0.0, 0.9, 0.46);
    vec3 successColor = vec3(0.0, 0.9, 0.46);

    float grid(vec2 uv, float cells) {
      vec2 g = abs(fract(uv * cells - 0.5) - 0.5) / fwidth(uv * cells);
      float line = min(g.x, g.y);
      return 1.0 - clamp(line, 0.0, 1.0);
    }

    void main() {
      vec3 base = idleColor * (0.5 + 0.5 * sin(uTime * 1.5));

      if (uState > 0.5 && uState < 1.5) {
        float pulse = 0.55 + 0.45 * sin(uTime * 3.0);
        base = hoverColor * pulse * 0.8;
      } else if (uState > 1.5 && uState < 2.5) {
        base = scanColor * 0.35;
        float band = smoothstep(0.06, 0.0, abs(vUv.y - (1.0 - uProgress)));
        base += scanColor * band * 2.2;
      } else if (uState > 2.5) {
        base = successColor * 0.9;
      }

      float g = grid(vUv, 10.0) * 0.15;
      vec3 color = base + g * (uState > 2.5 ? 0.4 : 1.0);

      float edge = smoothstep(0.0, 0.04, vUv.x) * smoothstep(1.0, 0.96, vUv.x) *
                   smoothstep(0.0, 0.04, vUv.y) * smoothstep(1.0, 0.96, vUv.y);
      color *= mix(0.6, 1.0, edge);

      gl_FragColor = vec4(color, 1.0);
    }
  `
);

extend({ ScanPlateMaterialImpl });

declare module "@react-three/fiber" {
  interface ThreeElements {
    scanPlateMaterialImpl: {
      ref?: React.Ref<THREE.ShaderMaterial>;
      uTime?: number;
      uState?: number;
      uProgress?: number;
      [key: string]: unknown;
    };
  }
}

export { ScanPlateMaterialImpl };
