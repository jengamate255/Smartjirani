import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { SignJWT } from "https://deno.land/x/jose@v4.14.4/index.ts";

const LIVEKIT_API_KEY = Deno.env.get("LIVEKIT_API_KEY")!;
const LIVEKIT_API_SECRET = Deno.env.get("LIVEKIT_API_SECRET")!;
const LIVEKIT_URL = Deno.env.get("LIVEKIT_URL")!;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
};

interface TokenRequest {
  roomName: string;
  participantName: string;
  participantId: string;
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    const { roomName, participantName, participantId }: TokenRequest =
      await req.json();

    if (!roomName || !participantName || !participantId) {
      throw new Error(
        "Missing required fields: roomName, participantName, participantId",
      );
    }

    const encoder = new TextEncoder();
    const secretBytes = encoder.encode(LIVEKIT_API_SECRET);

    const token = await new SignJWT({
      video: {
        room: roomName,
        roomJoin: true,
        canPublish: true,
        canSubscribe: true,
      },
      name: participantName,
    })
      .setProtectedHeader({ alg: "HS256" })
      .setIssuedAt()
      .setExpirationTime("6h")
      .setIssuer(LIVEKIT_API_KEY)
      .setSubject(participantId)
      .setJti(crypto.randomUUID())
      .sign(secretBytes);

    return new Response(
      JSON.stringify({
        token,
        url: LIVEKIT_URL,
        roomName,
      }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      },
    );
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 400,
      },
    );
  }
});
