"use client";

import { useEffect, useMemo, useRef, useState, useCallback } from "react";
import { Canvas, useFrame, useThree } from "@react-three/fiber";
import { PositionalAudio } from "@react-three/drei";
import * as THREE from "three";
import gsap from "gsap";
import "./ScanPlateMaterial";
import { synthChirpDataUri } from "@/lib/synthChirp";

type Phase = "idle" | "hover" | "scanning" | "success";

const SCAN_DURATION = 1.1;
const DOOR_WIDTH = 1.9;
const DOOR_X = 0.9;
const SCANNER_X = -0.55;
const WALL_Z = 0;

/* ------------------------------------------------------------------ */
/* Corridor: walls, floor, ceiling, LED strip                          */
/* ------------------------------------------------------------------ */
function Corridor() {
  return (
    <group>
      {/* back wall */}
      <mesh position={[0, 1.6, WALL_Z]} receiveShadow>
        <planeGeometry args={[8, 3.4]} />
        <meshStandardMaterial color="#2b2e33" roughness={0.85} metalness={0.1} />
      </mesh>

      {/* side walls */}
      <mesh position={[-4, 1.6, 3]} rotation={[0, Math.PI / 2, 0]}>
        <planeGeometry args={[6, 3.4]} />
        <meshStandardMaterial color="#24262a" roughness={0.9} />
      </mesh>
      <mesh position={[4, 1.6, 3]} rotation={[0, -Math.PI / 2, 0]}>
        <planeGeometry args={[6, 3.4]} />
        <meshStandardMaterial color="#24262a" roughness={0.9} />
      </mesh>

      {/* floor */}
      <mesh position={[0, 0, 3]} rotation={[-Math.PI / 2, 0, 0]} receiveShadow>
        <planeGeometry args={[8, 6]} />
        <meshStandardMaterial color="#1c1d20" roughness={0.6} metalness={0.2} />
      </mesh>

      {/* ceiling */}
      <mesh position={[0, 3.3, 3]} rotation={[Math.PI / 2, 0, 0]}>
        <planeGeometry args={[8, 6]} />
        <meshStandardMaterial color="#17181a" roughness={0.95} />
      </mesh>

      {/* LED strip */}
      <mesh position={[0, 3.25, 3]}>
        <boxGeometry args={[0.12, 0.03, 5.8]} />
        <meshStandardMaterial
          color="#bfe9ff"
          emissive="#bfe9ff"
          emissiveIntensity={2.2}
          toneMapped={false}
        />
      </mesh>
    </group>
  );
}

/* ------------------------------------------------------------------ */
/* Door: pivoted at the hinge so it swings open correctly              */
/* ------------------------------------------------------------------ */
function Door({ groupRef }: { groupRef: React.RefObject<THREE.Group | null> }) {
  const hingeX = DOOR_X - DOOR_WIDTH / 2;

  return (
    <group ref={groupRef} position={[hingeX, 0, WALL_Z + 0.03]}>
      <group position={[DOOR_WIDTH / 2, 1.55, 0]}>
        {/* glass panel */}
        <mesh castShadow>
          <boxGeometry args={[DOOR_WIDTH - 0.12, 2.9, 0.05]} />
          <meshPhysicalMaterial
            color="#dbe9f2"
            transmission={0.92}
            thickness={0.2}
            roughness={0.08}
            metalness={0}
            ior={1.4}
            attenuationColor="#bcd6e6"
            attenuationDistance={1}
          />
        </mesh>

        {/* aluminum frame */}
        {[
          [0, 1.47, 0.05, DOOR_WIDTH, 0.08],
          [0, -1.47, 0.05, DOOR_WIDTH, 0.08],
          [-(DOOR_WIDTH - 0.08) / 2, 0, 0.05, 0.08, 2.94],
          [(DOOR_WIDTH - 0.08) / 2, 0, 0.05, 0.08, 2.94],
        ].map(([x, y, z, w, h], i) => (
          <mesh key={i} position={[x, y, z]}>
            <boxGeometry args={[w, h, 0.09]} />
            <meshStandardMaterial color="#9aa2ab" roughness={0.3} metalness={0.85} />
          </mesh>
        ))}

        {/* vertical handle */}
        <mesh position={[DOOR_WIDTH / 2 - 0.28, 0, 0.09]} rotation={[Math.PI / 2, 0, 0]}>
          <cylinderGeometry args={[0.02, 0.02, 1.1, 16]} />
          <meshStandardMaterial color="#e6e9ec" roughness={0.2} metalness={0.95} />
        </mesh>
      </group>
    </group>
  );
}

/* ------------------------------------------------------------------ */
/* Scanner hub + scan plate                                            */
/* ------------------------------------------------------------------ */
const Scanner = ({
  onHoverChange,
  materialRef,
  hitRef,
}: {
  onHoverChange: (hovering: boolean) => void;
  materialRef: React.RefObject<THREE.ShaderMaterial | null>;
  hitRef: React.RefObject<THREE.Mesh | null>;
}) => {
  return (
    <group position={[SCANNER_X, 1.35, WALL_Z + 0.02]}>
      {/* hub housing */}
      <mesh castShadow>
        <boxGeometry args={[0.34, 0.5, 0.06]} />
        <meshStandardMaterial color="#3c3f44" roughness={0.35} metalness={0.9} />
      </mesh>

      {/* scan plate (visual only) */}
      <mesh position={[0, 0, 0.035]}>
        <planeGeometry args={[0.2, 0.32]} />
        <scanPlateMaterialImpl ref={materialRef} />
      </mesh>

      {/* generous invisible hit-area so the small plate is easy to target */}
      <mesh
        ref={hitRef}
        position={[0, 0, 0.04]}
        onPointerOver={(e) => {
          e.stopPropagation();
          onHoverChange(true);
        }}
        onPointerOut={(e) => {
          e.stopPropagation();
          onHoverChange(false);
        }}
      >
        <planeGeometry args={[0.42, 0.56]} />
        <meshBasicMaterial transparent opacity={0} depthWrite={false} />
      </mesh>
    </group>
  );
};

/* ------------------------------------------------------------------ */
/* Custom 3D finger cursor                                             */
/* ------------------------------------------------------------------ */
function FingerCursor({ phase }: { phase: Phase }) {
  const ref = useRef<THREE.Group>(null);
  const plane = useMemo(
    () => new THREE.Plane(new THREE.Vector3(0, 0, 1), -(WALL_Z + 0.12)),
    []
  );
  const raycaster = useMemo(() => new THREE.Raycaster(), []);
  const hit = useMemo(() => new THREE.Vector3(), []);

  useFrame(({ camera, pointer }) => {
    if (!ref.current) return;
    raycaster.setFromCamera(pointer, camera);
    if (raycaster.ray.intersectPlane(plane, hit)) {
      ref.current.position.lerp(hit, 0.35);
    }

    const pressed = phase === "scanning" || phase === "success";
    const targetTilt = phase === "hover" ? -0.5 : pressed ? -1.1 : -0.9;
    ref.current.rotation.x = THREE.MathUtils.lerp(
      ref.current.rotation.x,
      targetTilt,
      0.2
    );
    const targetScaleZ = pressed ? 0.4 : 1;
    ref.current.scale.z = THREE.MathUtils.lerp(ref.current.scale.z, targetScaleZ, 0.25);
  });

  const noRaycast = useCallback(() => null, []);

  return (
    <group ref={ref}>
      <mesh
        position={[0, 0, -0.05]}
        rotation={[Math.PI / 2, 0, 0]}
        raycast={noRaycast}
      >
        <capsuleGeometry args={[0.018, 0.09, 4, 8]} />
        <meshStandardMaterial
          color="#e8f4ff"
          transparent
          opacity={0.55}
          roughness={0.3}
          metalness={0.1}
        />
      </mesh>
      <mesh position={[0, 0, -0.11]} raycast={noRaycast}>
        <sphereGeometry args={[0.02, 12, 12]} />
        <meshStandardMaterial color="#ffffff" transparent opacity={0.75} />
      </mesh>
    </group>
  );
}

/* ------------------------------------------------------------------ */
/* Gym interior revealed once the door opens                           */
/* ------------------------------------------------------------------ */
function ClockPanel() {
  const canvas = useMemo(() => {
    const c = document.createElement("canvas");
    c.width = 256;
    c.height = 96;
    return c;
  }, []);
  const texture = useMemo(() => new THREE.CanvasTexture(canvas), [canvas]);

  useFrame(() => {
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.fillStyle = "#1a1206";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = "#ffb84d";
    ctx.font = "bold 44px monospace";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.fillText(new Date().toLocaleTimeString(), canvas.width / 2, canvas.height / 2);
    texture.needsUpdate = true;
  });

  return (
    <mesh position={[2.4, 2, 2.4]} rotation={[0, -Math.PI / 2, 0]}>
      <planeGeometry args={[0.9, 0.34]} />
      <meshBasicMaterial map={texture} toneMapped={false} />
    </mesh>
  );
}

function GymInterior() {
  const equipment: [number, number, number, number, number, number][] = [
    [-1.2, 0.55, 4.2, 0.5, 1.1, 0.5],
    [-0.3, 0.4, 4.6, 0.4, 0.8, 0.4],
    [0.7, 0.5, 4.3, 0.6, 1.0, 0.5],
    [1.6, 0.35, 4.7, 0.4, 0.7, 0.4],
  ];

  return (
    <group>
      <mesh position={[0, 0.01, 4.3]} rotation={[-Math.PI / 2, 0, 0]}>
        <planeGeometry args={[6, 3]} />
        <meshStandardMaterial color="#3a2a16" roughness={0.7} />
      </mesh>

      <pointLight position={[0, 2.4, 4]} intensity={18} color="#ffb066" distance={7} />
      <pointLight position={[-1.5, 2.2, 5]} intensity={10} color="#ffcf8a" distance={6} />

      {equipment.map(([x, y, z, w, h, d], i) => (
        <mesh key={i} position={[x, y, z]} castShadow>
          <boxGeometry args={[w, h, d]} />
          <meshStandardMaterial color="#151515" roughness={0.6} metalness={0.3} />
        </mesh>
      ))}

      <ClockPanel />
    </group>
  );
}

/* ------------------------------------------------------------------ */
/* Camera rig: subtle first-person parallax + GSAP reveal pan          */
/* ------------------------------------------------------------------ */
function CameraRig({ phase }: { phase: Phase }) {
  const { camera } = useThree();
  const lookTarget = useRef(new THREE.Vector3(0, 1.5, WALL_Z));
  const basePos = useRef(new THREE.Vector3(0, 1.6, 4.6));

  useEffect(() => {
    camera.position.copy(basePos.current);
  }, [camera]);

  useEffect(() => {
    if (phase !== "success") return;
    gsap.to(basePos.current, {
      x: 0.9,
      z: 3.1,
      duration: 2,
      delay: 0.5,
      ease: "power2.inOut",
    });
    gsap.to(lookTarget.current, {
      x: DOOR_X + 0.6,
      z: 4.2,
      duration: 2,
      delay: 0.5,
      ease: "power2.inOut",
    });
  }, [phase]);

  useFrame(({ pointer }) => {
    // Freeze parallax once the user is engaging with the scanner so the
    // (small) hit target doesn't drift out from under the cursor.
    const allowParallax = phase === "idle";
    const parallaxX = allowParallax ? basePos.current.x + pointer.x * 0.12 : camera.position.x;
    const parallaxY = allowParallax ? basePos.current.y + pointer.y * 0.06 : camera.position.y;
    camera.position.x = THREE.MathUtils.lerp(camera.position.x, parallaxX, 0.06);
    camera.position.y = THREE.MathUtils.lerp(camera.position.y, parallaxY, 0.06);
    camera.position.z = THREE.MathUtils.lerp(camera.position.z, basePos.current.z, 0.06);
    camera.lookAt(lookTarget.current);
  });

  return null;
}

/* ------------------------------------------------------------------ */
/* Manual press detection                                              */
/*                                                                     */
/* R3F's synthetic pointerdown/click pipeline can be unreliable with   */
/* some automated/synthetic input sources, so the "press and hold"     */
/* gesture is driven by a native listener + a direct raycast against   */
/* the scan plate's hit-area, independent of R3F's event system.       */
/* Hover (onPointerOver/Out) still uses R3F's built-in handling.       */
/* ------------------------------------------------------------------ */
function ManualPressDetector({
  hitRef,
  onPress,
}: {
  hitRef: React.RefObject<THREE.Mesh | null>;
  onPress: () => void;
}) {
  const { gl, camera } = useThree();

  useEffect(() => {
    const canvasEl = gl.domElement;
    const raycaster = new THREE.Raycaster();
    const ndc = new THREE.Vector2();

    const handleDown = (event: PointerEvent) => {
      const target = hitRef.current;
      if (!target) return;
      const liveCanvas = document.querySelector("canvas") ?? canvasEl;
      const rect = liveCanvas.getBoundingClientRect();
      ndc.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
      ndc.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;
      raycaster.setFromCamera(ndc, camera);
      const hits = raycaster.intersectObject(target, false);
      if (hits.length > 0) onPress();
    };

    window.addEventListener("pointerdown", handleDown, true);
    return () => window.removeEventListener("pointerdown", handleDown, true);
  }, [gl, camera, hitRef, onPress]);

  return null;
}

/* ------------------------------------------------------------------ */
/* Scene orchestration                                                 */
/* ------------------------------------------------------------------ */
function Scene({
  phase,
  setPhase,
}: {
  phase: Phase;
  setPhase: (p: Phase) => void;
}) {
  const doorGroupRef = useRef<THREE.Group>(null);
  const plateMaterialRef = useRef<THREE.ShaderMaterial>(null);
  const hitRef = useRef<THREE.Mesh>(null);
  const audioRef = useRef<THREE.PositionalAudio>(null);
  const scanStart = useRef<number | null>(null);
  const successTriggered = useRef(false);
  const scanTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);
  const chirpUrl = useMemo(() => synthChirpDataUri(), []);

  const triggerSuccess = useCallback(() => {
    if (successTriggered.current) return;
    successTriggered.current = true;
    setPhase("success");
    audioRef.current?.play();

    if (doorGroupRef.current) {
      gsap.to(doorGroupRef.current.rotation, {
        y: -Math.PI / 2,
        duration: 1.5,
        ease: "power2.inOut",
      });
    }
  }, [setPhase]);

  const handlePress = useCallback(() => {
    if (phase === "success") return;
    scanStart.current = performance.now();
    setPhase("scanning");
    // Timer-based completion (not rAF/useFrame) so the scan reliably
    // completes even if the render loop is starved during the hold.
    if (scanTimeout.current) clearTimeout(scanTimeout.current);
    scanTimeout.current = setTimeout(triggerSuccess, SCAN_DURATION * 1000);
  }, [phase, setPhase, triggerSuccess]);

  const cancelScan = useCallback(() => {
    if (phase === "scanning") {
      scanStart.current = null;
      if (scanTimeout.current) {
        clearTimeout(scanTimeout.current);
        scanTimeout.current = null;
      }
      setPhase("hover");
    }
  }, [phase, setPhase]);

  useEffect(() => {
    window.addEventListener("pointerup", cancelScan);
    return () => window.removeEventListener("pointerup", cancelScan);
  }, [cancelScan]);

  const handleHoverChange = useCallback(
    (hovering: boolean) => {
      if (phase === "idle" && hovering) setPhase("hover");
      else if (phase === "hover" && !hovering) setPhase("idle");
    },
    [phase, setPhase]
  );

  useFrame(({ clock }) => {
    const mat = plateMaterialRef.current;
    if (!mat) return;
    mat.uniforms.uTime.value = clock.getElapsedTime();

    const stateMap: Record<Phase, number> = {
      idle: 0,
      hover: 1,
      scanning: 2,
      success: 3,
    };
    mat.uniforms.uState.value = stateMap[phase];

    if (phase === "scanning" && scanStart.current !== null) {
      const progress = Math.min(
        (performance.now() - scanStart.current) / (SCAN_DURATION * 1000),
        1
      );
      mat.uniforms.uProgress.value = progress;
      if (progress >= 1) triggerSuccess();
    } else if (phase === "success") {
      mat.uniforms.uProgress.value = 1;
    } else {
      mat.uniforms.uProgress.value = 0;
    }
  });

  return (
    <>
      <ambientLight intensity={0.35} />
      <directionalLight position={[2, 4, 3]} intensity={0.5} color="#cfe0ff" />

      <Corridor />
      <Door groupRef={doorGroupRef} />
      <Scanner
        onHoverChange={handleHoverChange}
        materialRef={plateMaterialRef}
        hitRef={hitRef}
      />
      <ManualPressDetector hitRef={hitRef} onPress={handlePress} />
      <GymInterior />
      <FingerCursor phase={phase} />
      <CameraRig phase={phase} />

      <PositionalAudio ref={audioRef} url={chirpUrl} distance={3} loop={false} />
    </>
  );
}

/* ------------------------------------------------------------------ */
/* Top-level export                                                    */
/* ------------------------------------------------------------------ */
export default function ScannerExperience() {
  const [phase, setPhase] = useState<Phase>("idle");

  useEffect(() => {
    const prevCursor = document.body.style.cursor;
    document.body.style.cursor = "none";
    return () => {
      document.body.style.cursor = prevCursor;
    };
  }, []);

  const handleReset = () => {
    setPhase("idle");
    window.location.reload();
  };

  const statusText: Record<Phase, string> = {
    idle: "Move toward the scan plate to begin",
    hover: "Hold the scan plate to unlock",
    scanning: "Scanning… keep holding",
    success: "Access granted — welcome to GymSuite",
  };

  return (
    <div style={{ position: "relative", width: "100%", height: "100%" }}>
      <Canvas shadows camera={{ fov: 60, position: [0, 1.6, 4.6] }}>
        <color attach="background" args={["#0a0b0d"]} />
        <fog attach="fog" args={["#0a0b0d", 6, 14]} />
        <Scene phase={phase} setPhase={setPhase} />
      </Canvas>

      <div
        style={{
          position: "absolute",
          top: "1.5rem",
          left: "50%",
          transform: "translateX(-50%)",
          padding: "0.5rem 1.25rem",
          borderRadius: "999px",
          background: "rgba(10, 11, 13, 0.65)",
          color:
            phase === "success"
              ? "#00e676"
              : phase === "scanning"
              ? "#00e676"
              : phase === "hover"
              ? "#00b0ff"
              : "#e8e8e8",
          fontFamily: "monospace",
          fontSize: "0.9rem",
          letterSpacing: "0.02em",
          pointerEvents: "none",
        }}
      >
        {statusText[phase]}
      </div>

      {phase === "success" && (
        <button
          onClick={handleReset}
          style={{
            position: "absolute",
            bottom: "1.5rem",
            left: "50%",
            transform: "translateX(-50%)",
            padding: "0.6rem 1.25rem",
            borderRadius: "999px",
            border: "none",
            background: "#1f5fbf",
            color: "#fff",
            fontWeight: 600,
            fontSize: "0.85rem",
            cursor: "pointer",
          }}
        >
          Reset Demo
        </button>
      )}
    </div>
  );
}
