import type { MetadataRoute } from "next";

export const dynamic = "force-static";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "SmartJirani",
    short_name: "SmartJirani",
    description:
      "One app for Kenya's neighborhoods — barazas, harambees, trusted fundis, urgent help and everyday sharing.",
    start_url: "/",
    display: "standalone",
    background_color: "#FBF9F5",
    theme_color: "#135D43",
    icons: [{ src: "/icon.svg", sizes: "any", type: "image/svg+xml" }],
  };
}
