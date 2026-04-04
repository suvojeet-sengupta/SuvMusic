# WebSocket Protocol Implementation

<cite>
**Referenced Files in This Document**
- [Protocol.kt](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/Protocol.kt)
- [ListenTogetherClient.kt](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt)
- [MessageCodec.kt](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/MessageCodec.kt)
- [shareplay.proto](file://app/src/main/proto/shareplay.proto)
- [ListenTogetherEvent.kt](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherEvent.kt)
- [ListenTogetherServers.kt](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherServers.kt)
- [ListenTogetherManager.kt](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherManager.kt)
- [ListenTogetherActionReceiver.kt](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherActionReceiver.kt)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Dependency Analysis](#dependency-analysis)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Conclusion](#conclusion)

## Introduction
This document provides comprehensive documentation for the WebSocket-based protocol implementation that enables real-time synchronization between devices in a Listen Together session. The implementation consists of a robust client-server protocol built on Protocol Buffers for efficient serialization, with sophisticated connection lifecycle management, reconnection strategies, and error handling patterns.

The protocol supports synchronized music playback across multiple devices, user management, room moderation, and real-time chat functionality. It features advanced capabilities including buffer management for guests, drift correction for precise timing, and comprehensive state synchronization.

## Project Structure
The WebSocket protocol implementation is organized around several key components:

```mermaid
graph TB
subgraph "Protocol Layer"
A[Protocol.kt<br/>Message Types & Payloads]
B[shareplay.proto<br/>Protocol Buffer Definitions]
end
subgraph "Transport Layer"
C[MessageCodec.kt<br/>Encoding/Decoding]
D[ListenTogetherClient.kt<br/>WebSocket Client]
end
subgraph "Application Layer"
E[ListenTogetherManager.kt<br/>Player Integration]
F[ListenTogetherEvent.kt<br/>Event System]
G[ListenTogetherServers.kt<br/>Server Configuration]
H[ListenTogetherActionReceiver.kt<br/>Notification Actions]
end
A --> C
B --> C
C --> D
D --> E
E --> F
G --> D
H --> D
```

**Diagram sources**
- [Protocol.kt:1-320](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/Protocol.kt#L1-L320)
- [MessageCodec.kt:1-355](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/MessageCodec.kt#L1-L355)
- [ListenTogetherClient.kt:1-1205](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L1-L1205)

**Section sources**
- [Protocol.kt:1-320](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/Protocol.kt#L1-L320)
- [MessageCodec.kt:1-355](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/MessageCodec.kt#L1-L355)
- [ListenTogetherClient.kt:1-1205](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L1-L1205)

## Core Components

### Protocol Specification
The protocol defines a comprehensive set of message types and payload structures for real-time synchronization:

**Message Types** are categorized into client-to-server and server-to-client communications:
- **Client -> Server**: create_room, join_room, leave_room, approve_join, reject_join, playback_action, buffer_ready, kick_user, transfer_host, ping, chat, request_sync, reconnect, suggest_track, approve_suggestion, reject_suggestion
- **Server -> Client**: room_created, join_request, join_approved, join_rejected, user_joined, user_left, sync_playback, buffer_wait, buffer_complete, error, pong, host_changed, kicked, sync_state, reconnected, user_reconnected, user_disconnected, suggestion_received, suggestion_approved, suggestion_rejected

**Playback Actions** include comprehensive media control operations:
- Play/Pause operations with position tracking
- Seek operations with precise timing
- Track switching with queue management
- Queue operations (add, remove, clear, sync)
- Volume control with synchronization
- Skip operations (next/previous)

**Data Structures** define the complete state model:
- TrackInfo: Complete metadata for audio tracks
- UserInfo: User identity and role information
- RoomState: Complete room synchronization state
- Comprehensive payload structures for all message types

**Section sources**
- [Protocol.kt:9-49](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/Protocol.kt#L9-L49)
- [Protocol.kt:54-66](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/Protocol.kt#L54-L66)
- [Protocol.kt:71-107](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/Protocol.kt#L71-L107)

### MessageCodec Implementation
The MessageCodec provides efficient Protocol Buffer serialization with automatic compression:

```mermaid
sequenceDiagram
participant Client as "ListenTogetherClient"
participant Codec as "MessageCodec"
participant PB as "Protocol Buffers"
participant WS as "WebSocket"
Client->>Codec : encode(messageType, payload)
Codec->>PB : toProtoMessage(payload)
PB-->>Codec : Serialized bytes
Codec->>Codec : Compress if > 100 bytes
Codec->>Codec : Wrap in Envelope
Codec-->>Client : Final bytes
Client->>WS : send(encodedBytes)
```

**Diagram sources**
- [MessageCodec.kt:30-84](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/MessageCodec.kt#L30-L84)
- [shareplay.proto:10-15](file://app/src/main/proto/shareplay.proto#L10-L15)

**Section sources**
- [MessageCodec.kt:19-355](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/MessageCodec.kt#L19-L355)
- [shareplay.proto:10-223](file://app/src/main/proto/shareplay.proto#L10-L223)

### ListenTogetherClient Architecture
The ListenTogetherClient serves as the central WebSocket management component:

```mermaid
classDiagram
class ListenTogetherClient {
-OkHttpClient client
-WebSocket webSocket
-MessageCodec messageCodec
-CoroutineScope scope
-Job pingJob
-Job reconnectJob
-String sessionToken
-MutableStateFlow~ConnectionState~ connectionState
-MutableStateFlow~RoomState~ roomState
-MutableStateFlow~RoomRole~ role
+connect()
+disconnect()
+createRoom(username)
+joinRoom(roomCode, username)
+leaveRoom()
+sendPlaybackAction(...)
+approveJoin(userId)
+rejectJoin(userId, reason)
+kickUser(userId, reason)
+transferHost(newHostId)
+suggestTrack(trackInfo)
+approveSuggestion(suggestionId)
+rejectSuggestion(suggestionId, reason)
+requestSync()
+forceReconnect()
}
class ConnectionState {
<<enumeration>>
DISCONNECTED
CONNECTING
CONNECTED
RECONNECTING
ERROR
}
class RoomRole {
<<enumeration>>
HOST
GUEST
NONE
}
ListenTogetherClient --> ConnectionState
ListenTogetherClient --> RoomRole
ListenTogetherClient --> MessageCodec
ListenTogetherClient --> OkHttpClient
ListenTogetherClient --> WebSocket
```

**Diagram sources**
- [ListenTogetherClient.kt:66-82](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L66-L82)
- [ListenTogetherClient.kt:111-144](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L111-L144)

**Section sources**
- [ListenTogetherClient.kt:111-1205](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L111-L1205)

## Architecture Overview

### Connection Lifecycle Management
The client implements sophisticated connection lifecycle management with exponential backoff and graceful degradation:

```mermaid
stateDiagram-v2
[*] --> Disconnected
Disconnected --> Connecting : connect()
Connecting --> Connected : WebSocket Open
Connecting --> Error : Connection Failure
Connected --> Reconnecting : Connection Lost
Reconnecting --> Connected : Reconnect Success
Reconnecting --> Error : Max Attempts Reached
Connected --> Disconnected : disconnect()
Error --> Disconnected : Manual Disconnect
Disconnected --> Reconnecting : Auto-Reconnect
```

**Diagram sources**
- [ListenTogetherClient.kt:66-72](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L66-L72)
- [ListenTogetherClient.kt:119-124](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L119-L124)

### Message Flow Architecture
The system processes incoming messages through a comprehensive handler:

```mermaid
flowchart TD
A[WebSocket Message] --> B[MessageCodec.decode]
B --> C{Message Type}
C --> |ROOM_CREATED| D[Create Room Handler]
C --> |JOIN_REQUEST| E[Join Request Handler]
C --> |JOIN_APPROVED| F[Join Approved Handler]
C --> |SYNC_PLAYBACK| G[Playback Sync Handler]
C --> |BUFFER_WAIT| H[Buffer Wait Handler]
C --> |ERROR| I[Error Handler]
C --> |RECONNECTED| J[Reconnect Handler]
D --> K[Update Room State]
E --> L[Manage Join Requests]
F --> M[Initialize Room State]
G --> N[Apply Playback Changes]
H --> O[Update Buffering Status]
I --> P[Handle Error Conditions]
J --> Q[Restore Session State]
K --> R[Emit Events]
L --> R
M --> R
N --> R
O --> R
P --> R
Q --> R
```

**Diagram sources**
- [ListenTogetherClient.kt:704-1020](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L704-L1020)
- [MessageCodec.kt:180-304](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/MessageCodec.kt#L180-L304)

**Section sources**
- [ListenTogetherClient.kt:411-702](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L411-L702)
- [ListenTogetherClient.kt:704-1020](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L704-L1020)

## Detailed Component Analysis

### Protocol Buffer Definitions
The Protocol Buffer schema provides efficient binary serialization with comprehensive type safety:

```mermaid
erDiagram
Envelope {
string type PK
bytes payload
bool compressed
}
TrackInfo {
string id PK
string title
string artist
string album
int64 duration
string thumbnail
string suggested_by
}
UserInfo {
string user_id PK
string username
bool is_host
bool is_connected
}
RoomState {
string room_code PK
string host_id
bool is_playing
int64 position
int64 last_update
float volume
}
Envelope ||--|| TrackInfo : "contains"
RoomState ||--o{ UserInfo : "has"
RoomState ||--o{ TrackInfo : "queued"
```

**Diagram sources**
- [shareplay.proto:10-47](file://app/src/main/proto/shareplay.proto#L10-L47)

**Section sources**
- [shareplay.proto:10-223](file://app/src/main/proto/shareplay.proto#L10-L223)

### Authentication and Session Management
The client implements robust session persistence and authentication:

```mermaid
sequenceDiagram
participant Client as "Client"
participant DataStore as "DataStore"
participant Server as "Server"
Client->>DataStore : Load Persisted Session
DataStore-->>Client : Session Token, Room Code, Timestamp
Client->>Server : Connect WebSocket
Server-->>Client : Connection Established
alt Session Exists & Valid
Client->>Server : RECONNECT with Session Token
Server-->>Client : RECONNECTED with Room State
Client->>Client : Restore Session State
else No Session or Expired
Client->>Server : CREATE_ROOM or JOIN_ROOM
Server-->>Client : ROOM_CREATED or JOIN_APPROVED
Client->>DataStore : Save Session Information
end
```

**Diagram sources**
- [ListenTogetherClient.kt:157-227](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L157-L227)
- [Protocol.kt:296-320](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/Protocol.kt#L296-L320)

**Section sources**
- [ListenTogetherClient.kt:157-227](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L157-L227)
- [ListenTogetherClient.kt:294-314](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L294-L314)

### Playback Synchronization Engine
The ListenTogetherManager integrates with the media player for seamless synchronization:

```mermaid
flowchart TD
A[Host Action] --> B[ListenTogetherManager]
B --> C[Send Playback Action]
C --> D[WebSocket Message]
D --> E[Other Clients]
E --> F[Apply Changes]
G[Guest Receives Sync] --> H[Buffer Management]
H --> I[Prepare Media]
I --> J[Send Buffer Ready]
J --> K[Start Playback]
L[Drift Correction] --> M[Monitor Position]
M --> N{Drift Detected?}
N --> |Yes| O[Adjust Speed/Seek]
N --> |No| P[Maintain Normal Speed]
```

**Diagram sources**
- [ListenTogetherManager.kt:229-556](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherManager.kt#L229-L556)
- [ListenTogetherManager.kt:336-380](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherManager.kt#L336-L380)

**Section sources**
- [ListenTogetherManager.kt:229-556](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherManager.kt#L229-L556)
- [ListenTogetherManager.kt:336-380](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherManager.kt#L336-L380)

### Reconnection Strategy
The client implements sophisticated reconnection logic with exponential backoff:

```mermaid
flowchart TD
A[Connection Lost] --> B{Has Session?}
B --> |Yes| C[Start Reconnection]
B --> |No| D[Clear State]
C --> E[Calculate Backoff Delay]
E --> F[Exponential Backoff]
F --> G[Jitter Added]
G --> H[Attempt Reconnect]
H --> I{Success?}
I --> |Yes| J[Restore Session]
I --> |No| K{Attempts < Max?}
K --> |Yes| E
K --> |No| L[Mark as Error]
J --> M[Resume Operations]
D --> N[Notify Disconnected]
L --> O[Notify Connection Error]
```

**Diagram sources**
- [ListenTogetherClient.kt:652-702](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L652-L702)
- [ListenTogetherClient.kt:342-347](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L342-L347)

**Section sources**
- [ListenTogetherClient.kt:652-702](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L652-L702)
- [ListenTogetherClient.kt:342-347](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L342-L347)

## Dependency Analysis

### Component Dependencies
The system exhibits clean separation of concerns with minimal coupling:

```mermaid
graph TB
subgraph "External Dependencies"
A[OkHttp WebSocket]
B[Protocol Buffers]
C[Kotlin Coroutines]
D[Android DataStore]
end
subgraph "Internal Components"
E[MessageCodec]
F[ListenTogetherClient]
G[ListenTogetherManager]
H[Protocol Definitions]
end
E --> B
F --> A
F --> C
F --> D
G --> F
G --> C
H --> E
H --> F
```

**Diagram sources**
- [MessageCodec.kt:8-14](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/MessageCodec.kt#L8-L14)
- [ListenTogetherClient.kt:34-44](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L34-L44)

### Protocol Versioning
The protocol includes explicit versioning support through capability negotiation:

```mermaid
sequenceDiagram
participant Client as "Client"
participant Server as "Server"
Client->>Server : ClientCapabilities
Note over Client,Server : supports_protobuf : true<br/>supports_compression : true<br/>client_version : "1.0.0"
Server-->>Client : ServerCapabilities
Note over Client,Server : supports_protobuf : true<br/>supports_compression : true<br/>server_version : "1.0.0"
Client->>Server : Protocol Messages
Server-->>Client : Protocol Responses
```

**Diagram sources**
- [shareplay.proto:211-223](file://app/src/main/proto/shareplay.proto#L211-L223)

**Section sources**
- [shareplay.proto:211-223](file://app/src/main/proto/shareplay.proto#L211-L223)

**Section sources**
- [MessageCodec.kt:19-355](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/MessageCodec.kt#L19-L355)
- [ListenTogetherClient.kt:111-1205](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L111-L1205)

## Performance Considerations

### Compression Strategy
The MessageCodec implements intelligent compression for large payloads:
- Automatic compression threshold of 100 bytes
- GZIP compression for payloads exceeding threshold
- Transparent decompression during decoding
- Configurable compression enablement

### Connection Optimization
- WebSocket ping/pong mechanism for connection health monitoring
- Optimized timeouts for different socket operations
- Efficient state updates to minimize bandwidth usage
- Background processing for non-critical operations

### Memory Management
- State flows for reactive UI updates
- Proper resource cleanup on disconnection
- Memory-efficient Protocol Buffer usage
- Controlled logging to prevent memory leaks

## Troubleshooting Guide

### Common Connection Issues
**Connection Refused**: Verify server URL configuration and network connectivity
**Authentication Failures**: Check session token validity and expiration
**Reconnection Loops**: Review exponential backoff parameters and network stability
**Message Serialization Errors**: Validate Protocol Buffer schema compatibility

### Debugging Tools
The client provides comprehensive logging infrastructure:
- Structured log entries with timestamps and severity levels
- Configurable log filtering and export capabilities
- Event-driven state tracking for debugging
- Session persistence inspection for troubleshooting

### Recovery Strategies
- Graceful degradation when server features are unavailable
- Automatic retry mechanisms with exponential backoff
- Session state preservation across reconnections
- User notification system for join requests and suggestions

**Section sources**
- [ListenTogetherClient.kt:393-409](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L393-L409)
- [ListenTogetherClient.kt:636-702](file://app/src/main/java/com/suvojeet/suvmusic/shareplay/ListenTogetherClient.kt#L636-L702)

## Conclusion
The WebSocket-based protocol implementation provides a robust foundation for real-time synchronized music playback across multiple devices. The architecture demonstrates excellent separation of concerns, with clear protocols for message exchange, sophisticated connection management, and comprehensive error handling.

Key strengths include:
- Efficient Protocol Buffer serialization with automatic compression
- Sophisticated reconnection strategies with exponential backoff
- Advanced playback synchronization with drift correction
- Comprehensive state management and session persistence
- Clean integration with Android media framework

The implementation successfully balances performance requirements with reliability, providing a solid foundation for the Listen Together feature while maintaining extensibility for future enhancements.