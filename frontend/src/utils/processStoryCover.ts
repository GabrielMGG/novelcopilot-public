const TARGET_WIDTH = 800;
const TARGET_HEIGHT = 1200;
const TARGET_RATIO = 2 / 3;
const WEBP_QUALITY = 0.82;

export async function processStoryCover(file: File): Promise<Blob> {
  const imageBitmap = await createImageBitmap(file);

  const { width: srcW, height: srcH } = imageBitmap;
  const srcRatio = srcW / srcH;

  let cropW: number;
  let cropH: number;

  if (srcRatio > TARGET_RATIO) {
    // Source is wider than 2:3 — crop sides
    cropH = srcH;
    cropW = Math.round(srcH * TARGET_RATIO);
  } else {
    // Source is taller than 2:3 — crop top/bottom
    cropW = srcW;
    cropH = Math.round(srcW / TARGET_RATIO);
  }

  const cropX = Math.round((srcW - cropW) / 2);
  const cropY = Math.round((srcH - cropH) / 2);

  const canvas = document.createElement("canvas");
  canvas.width = TARGET_WIDTH;
  canvas.height = TARGET_HEIGHT;

  const ctx = canvas.getContext("2d");
  if (!ctx) {
    throw new Error("Não foi possível criar o contexto do canvas.");
  }

  ctx.drawImage(
    imageBitmap,
    cropX,
    cropY,
    cropW,
    cropH,
    0,
    0,
    TARGET_WIDTH,
    TARGET_HEIGHT
  );

  imageBitmap.close();

  return new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          reject(new Error("O navegador não suporta conversão para WebP. Tente outro navegador."));
          return;
        }
        resolve(blob);
      },
      "image/webp",
      WEBP_QUALITY
    );
  });
}
