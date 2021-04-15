package com.stim.domain.models

sealed class ConnectionStatus {
    object Exception : ConnectionStatus()
    object SyncMasterError : ConnectionStatus()
    object MasterError : ConnectionStatus()
    object SyncError : ConnectionStatus()
    object Success : ConnectionStatus()
}