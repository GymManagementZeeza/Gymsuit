function encodeWav(samples: Float32Array, sampleRate: number): ArrayBuffer {
  const buffer = new ArrayBuffer(44 + samples.length * 2);
  const view = new DataView(buffer);

  const writeString = (offset: number, str: string) => {
    for (let i = 0; i < str.length; i++) {
      view.setUint8(offset + i, str.charCodeAt(i));
    }
  };

  writeString(0, "RIFF");
  view.setUint32(4, 36 + samples.length * 2, true);
  writeString(8, "WAVE");
  writeString(12, "fmt ");
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, sampleRate * 2, true);
  view.setUint16(32, 2, true);
  view.setUint16(34, 16, true);
  writeString(36, "data");
  view.setUint32(40, samples.length * 2, true);

  let offset = 44;
  for (let i = 0; i < samples.length; i++) {
    const s = Math.max(-1, Math.min(1, samples[i]));
    view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7fff, true);
    offset += 2;
  }

  return buffer;
}

/**
 * Synthesizes a short two-tone access-granted "chirp" and returns it as a
 * data: URI WAV file — no external audio asset needed.
 */
export function synthChirpDataUri(): string {
  const sampleRate = 44100;
  const duration = 0.28;
  const length = Math.floor(sampleRate * duration);
  const samples = new Float32Array(length);

  const tone = (
    startT: number,
    endT: number,
    freqStart: number,
    freqEnd: number
  ) => {
    const startI = Math.floor(startT * sampleRate);
    const endI = Math.floor(endT * sampleRate);
    for (let i = startI; i < endI && i < length; i++) {
      const t = (i - startI) / sampleRate;
      const localDur = (endI - startI) / sampleRate;
      const freq = freqStart + (freqEnd - freqStart) * (t / localDur);
      const envelope = Math.sin((Math.PI * (i - startI)) / (endI - startI));
      samples[i] += Math.sin(2 * Math.PI * freq * t) * envelope * 0.5;
    }
  };

  tone(0, 0.12, 880, 1046.5);
  tone(0.14, 0.28, 1318.5, 1568);

  const wav = encodeWav(samples, sampleRate);
  let binary = "";
  const bytes = new Uint8Array(wav);
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return `data:audio/wav;base64,${btoa(binary)}`;
}
