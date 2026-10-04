import SwiftUI

/// Top Cloud Synchronization Indicator:
/// 1. Line loading bar on top when syncing.
/// 2. Green "All data synced" message when synchronization succeeds.
public struct CloudSyncIndicatorView: View {
    @ObservedObject var syncManager: HealthSyncManager = .shared
    
    public init() {}
    
    public var body: some View {
        VStack(spacing: 0) {
            // Linear loading bar on top
            if syncManager.syncStatus == .syncing {
                LineLoadingBar()
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }
            
            // Floating message badge
            if syncManager.syncStatus != .idle {
                HStack {
                    Spacer()
                    
                    switch syncManager.syncStatus {
                    case .syncing:
                        HStack(spacing: 8) {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: Color(hex: 0xFF16A34A)))
                                .scaleEffect(0.75)
                            
                            Text("Syncing to cloud…")
                                .font(.system(size: 12.5, weight: .semibold))
                                .foregroundColor(Color(hex: 0xFF15803D))
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 7)
                        .background(Color(hex: 0xFFF0FDF4))
                        .clipShape(Capsule())
                        .overlay(Capsule().stroke(Color(hex: 0xFFBBF7D0), lineWidth: 1))
                        .shadow(color: Color.black.opacity(0.08), radius: 6, y: 2)
                        
                    case .success(let message):
                        HStack(spacing: 8) {
                            Image(systemName: "checkmark.circle.fill")
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(Color(hex: 0xFF16A34A))
                            
                            Text(message) // "All data synced"
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(Color(hex: 0xFF15803D))
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(Color(hex: 0xFFDCFCE7))
                        .clipShape(Capsule())
                        .overlay(Capsule().stroke(Color(hex: 0xFF86EFAC), lineWidth: 1))
                        .shadow(color: Color.black.opacity(0.1), radius: 8, y: 3)
                        
                    case .error(let message):
                        HStack(spacing: 8) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(Color(hex: 0xFFDC2626))
                            
                            Text(message)
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(Color(hex: 0xFFB91C1C))
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 7)
                        .background(Color(hex: 0xFFFEF2F2))
                        .clipShape(Capsule())
                        .overlay(Capsule().stroke(Color(hex: 0xFFFECACA), lineWidth: 1))
                        .shadow(color: Color.black.opacity(0.08), radius: 6, y: 2)
                        
                    case .idle:
                        EmptyView()
                    }
                    
                    Spacer()
                }
                .padding(.top, 8)
                .transition(.asymmetric(
                    insertion: .move(edge: .top).combined(with: .opacity),
                    removal: .move(edge: .top).combined(with: .opacity)
                ))
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.8), value: syncManager.syncStatus)
    }
}

/// A linear indeterminate loading line bar running on top.
struct LineLoadingBar: View {
    @State private var phase: CGFloat = 0
    
    var body: some View {
        GeometryReader { geo in
            let barWidth = max(40, geo.size.width * 0.35)
            ZStack(alignment: .leading) {
                Rectangle()
                    .fill(Color(hex: 0x3316A34A))
                
                Rectangle()
                    .fill(Color(hex: 0xFF16A34A))
                    .frame(width: barWidth)
                    .offset(x: phase * (geo.size.width + barWidth) - barWidth)
            }
        }
        .frame(height: 3.5)
        .clipped()
        .onAppear {
            withAnimation(.linear(duration: 1.1).repeatForever(autoreverses: false)) {
                phase = 1.0
            }
        }
    }
}
