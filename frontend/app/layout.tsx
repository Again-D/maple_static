import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./../styles/globals.css";

export const metadata: Metadata = {
  title: "Maple Growth Tracker",
  description: "Anonymous MapleStory character growth dashboard"
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="ko">
      <head>
        <script type="text/javascript" src="https://openapi.nexon.com/js/analytics.js?app_id=321861" async></script>
      </head>
      <body>{children}</body>
    </html>
  );
}
