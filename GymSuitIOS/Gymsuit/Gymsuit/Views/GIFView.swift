import SwiftUI
import WebKit

/// Plays a remote GIF (exercise animation) inside a WKWebView.
struct GIFView: UIViewRepresentable {
    let url: URL?

    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView()
        webView.scrollView.isScrollEnabled = false
        webView.isOpaque = false
        webView.backgroundColor = .clear
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        guard let url, webView.url != url else { return }
        webView.load(URLRequest(url: url))
    }
}
