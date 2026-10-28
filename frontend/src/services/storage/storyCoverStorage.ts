import { supabase } from "../../lib/supabaseClient";

export const STORY_COVERS_BUCKET = "story-covers";

const SIGNED_URL_EXPIRY_SECONDS = 3600; // 1 hour

export type UploadedStoryCover = {
  path: string;
};

export async function uploadStoryCover(
  imageBlob: Blob,
  userId: string,
  storyId: number
): Promise<UploadedStoryCover> {
  const uuid = crypto.randomUUID();
  const path = `${userId}/${storyId}/${uuid}.webp`;

  const { error } = await supabase.storage
    .from(STORY_COVERS_BUCKET)
    .upload(path, imageBlob, {
      contentType: "image/webp",
      upsert: false,
      cacheControl: "3600",
    });

  if (error) {
    throw new Error(`Falha ao enviar a capa: ${error.message}`);
  }

  return { path };
}

export async function createStoryCoverSignedUrl(
  path: string
): Promise<string> {
  const { data, error } = await supabase.storage
    .from(STORY_COVERS_BUCKET)
    .createSignedUrl(path, SIGNED_URL_EXPIRY_SECONDS);

  if (error || !data?.signedUrl) {
    throw new Error("Não foi possível gerar a URL da capa.");
  }

  return data.signedUrl;
}

export async function createStoryCoverSignedUrls(
  paths: string[]
): Promise<Map<string, string>> {
  const result = new Map<string, string>();

  if (paths.length === 0) return result;

  const { data, error } = await supabase.storage
    .from(STORY_COVERS_BUCKET)
    .createSignedUrls(paths, SIGNED_URL_EXPIRY_SECONDS);

  if (error || !data) {
    return result;
  }

  for (const item of data) {
    if (item.signedUrl && !item.error) {
      // The path in the response matches the input path
      const itemSignedUrl = item.signedUrl;
      const matchingPath = paths.find((p) => item.path?.endsWith(p) || itemSignedUrl.includes(encodeURIComponent(p)));
      if (matchingPath) {
        result.set(matchingPath, itemSignedUrl);
      }
    }
  }

  return result;
}

// NOTA ARQUITETURAL: Limpeza de imagens do Storage
// Quando uma história é enviada para a Lixeira, as imagens não são deletadas imediatamente.
// O Storage Supabase só é realmente limpo quando o usuário Esvazia a lixeira (ou após 7 dias
// via Edge Function / Job assíncrono programado). Isso permite restaurar a história com a imagem intacta.
export async function deleteStoryCover(
  path: string
): Promise<void> {
  const { error } = await supabase.storage
    .from(STORY_COVERS_BUCKET)
    .remove([path]);

  if (error) {
    console.error("Falha ao excluir a capa:", error.message);
  }
}