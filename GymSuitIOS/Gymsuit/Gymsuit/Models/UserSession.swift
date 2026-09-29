import Foundation

public struct UserSession: Codable, Equatable {
    public let token: String
    public let refreshToken: String
    public let email: String
    public var firstName: String
    public var lastName: String
    public let role: String
    public let memberId: Int64?
    public let gymId: Int64?
    public let gymName: String?
    
    public init(
        token: String,
        refreshToken: String = "",
        email: String = "",
        firstName: String = "",
        lastName: String = "",
        role: String = "MEMBER",
        memberId: Int64? = nil,
        gymId: Int64? = nil,
        gymName: String? = nil
    ) {
        self.token = token
        self.refreshToken = refreshToken
        self.email = email
        self.firstName = firstName
        self.lastName = lastName
        self.role = role
        self.memberId = memberId
        self.gymId = gymId
        self.gymName = gymName
    }
}
