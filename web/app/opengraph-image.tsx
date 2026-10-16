import { ImageResponse } from "next/og";

export const dynamic = "force-static";
export const alt = "SmartJirani — Your mtaa, connected";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default function OpengraphImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
          background: "linear-gradient(135deg, #00442F 0%, #135D43 60%, #1A6B4F 100%)",
          padding: 80,
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
          <div
            style={{
              width: 84,
              height: 84,
              borderRadius: 24,
              background: "#ABF1CF",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            <div
              style={{
                width: 44,
                height: 44,
                borderRadius: 14,
                background: "#00442F",
                display: "flex",
              }}
            />
          </div>
          <div style={{ fontSize: 56, fontWeight: 800, color: "#FFFFFF", display: "flex" }}>
            SmartJirani
          </div>
        </div>
        <div
          style={{
            marginTop: 48,
            fontSize: 76,
            fontWeight: 800,
            color: "#FFFFFF",
            lineHeight: 1.1,
            display: "flex",
          }}
        >
          Your mtaa, finally connected.
        </div>
        <div
          style={{
            marginTop: 28,
            fontSize: 34,
            color: "#ABF1CF",
            display: "flex",
          }}
        >
          Barazas · Harambees · Trusted fundis · Urgent help
        </div>
        <div
          style={{
            marginTop: 56,
            display: "flex",
            gap: 16,
          }}
        >
          <div
            style={{
              background: "#D95D39",
              color: "#FFFFFF",
              fontSize: 26,
              fontWeight: 700,
              padding: "14px 36px",
              borderRadius: 999,
              display: "flex",
            }}
          >
            Get the app
          </div>
          <div
            style={{
              border: "2px solid #ABF1CF",
              color: "#ABF1CF",
              fontSize: 26,
              fontWeight: 700,
              padding: "14px 36px",
              borderRadius: 999,
              display: "flex",
            }}
          >
            Free for residents
          </div>
        </div>
      </div>
    ),
    size
  );
}
