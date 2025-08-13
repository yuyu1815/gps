package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.UserPosition3D
import com.example.myapplication.domain.usecase.position.GetCurrentPositionUseCase
import com.example.myapplication.domain.usecase.position.GetPositionHistoryUseCase
import com.example.myapplication.domain.usecase.position.UpdatePositionUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 座標表示画面のViewModel
 */
class CoordinateDisplayViewModel(
    private val getCurrentPositionUseCase: GetCurrentPositionUseCase,
    private val getPositionHistoryUseCase: GetPositionHistoryUseCase,
    private val updatePositionUseCase: UpdatePositionUseCase,
    private val positionRepository: IPositionRepository
) : ViewModel() {

    // 現在の3D位置
    private val _currentPosition = MutableStateFlow<UserPosition3D?>(null)
    val currentPosition: StateFlow<UserPosition3D?> = _currentPosition.asStateFlow()

    // 3D位置履歴
    private val _positionHistory = MutableStateFlow<List<UserPosition3D>>(emptyList())
    val positionHistory: StateFlow<List<UserPosition3D>> = _positionHistory.asStateFlow()

    // ローディング状態
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // エラー状態
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 座標更新状態
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    // 更新間隔（ミリ秒）
    private var updateInterval = 1000L

    // 座標更新ジョブ
    private var coordinateUpdateJob: kotlinx.coroutines.Job? = null

    init {
        try {
            loadPositionHistory()
            startCoordinateUpdates()
            observe3DPositionUpdates()
        } catch (e: Exception) {
            Timber.e(e, "CoordinateDisplayViewModel初期化中にエラーが発生しました")
            _error.value = "初期化中にエラーが発生しました: ${e.message}"
        }
    }
    
    /**
     * 3D位置更新を監視
     */
    private fun observe3DPositionUpdates() {
        viewModelScope.launch {
            positionRepository.getCurrent3DPositionFlow().collect { position3D ->
                if (position3D.isValid()) {
                    _currentPosition.value = position3D
                    addToHistory(position3D)
                    _error.value = null
                    Timber.d("3D座標を更新しました: $position3D")
                } else {
                    _error.value = "有効な3D座標データが取得できませんでした"
                    Timber.w("無効な3D座標データが取得されました: $position3D")
                }
            }
        }
    }

    /**
     * 現在の位置を取得
     */
    fun refreshPosition() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // 3D位置を直接取得
                val position3D = positionRepository.getCurrent3DPosition()
                
                // 無効な位置データの場合はエラーを設定
                if (!position3D.isValid()) {
                    _error.value = "有効な3D座標データが取得できませんでした"
                    Timber.w("無効な3D座標データが取得されました: $position3D")
                    return@launch
                }
                
                _currentPosition.value = position3D
                
                // 履歴に追加
                addToHistory(position3D)
                
                Timber.d("3D座標を更新しました: $position3D")
            } catch (e: Exception) {
                _error.value = "3D座標の取得中にエラーが発生しました: ${e.message}"
                Timber.e(e, "3D座標の取得中にエラーが発生しました")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 座標更新を開始
     */
    fun startCoordinateUpdates() {
        if (_isUpdating.value) return

        _isUpdating.value = true
        coordinateUpdateJob = viewModelScope.launch {
            while (_isUpdating.value) {
                refreshPosition()
                kotlinx.coroutines.delay(updateInterval)
            }
        }
        Timber.d("3D座標更新を開始しました")
    }

    /**
     * 座標更新を停止
     */
    fun stopCoordinateUpdates() {
        _isUpdating.value = false
        coordinateUpdateJob?.cancel()
        coordinateUpdateJob = null
        Timber.d("3D座標更新を停止しました")
    }

    /**
     * 座標更新の開始/停止を切り替え
     */
    fun toggleCoordinateUpdates() {
        if (_isUpdating.value) {
            stopCoordinateUpdates()
        } else {
            startCoordinateUpdates()
        }
    }

    /**
     * 位置履歴を読み込み
     */
    private fun loadPositionHistory() {
        viewModelScope.launch {
            try {
                val history = positionRepository.get3DPositionHistory(1000)
                _positionHistory.value = history
                Timber.d("3D位置履歴を読み込みました: ${history.size}件")
            } catch (e: Exception) {
                Timber.e(e, "3D位置履歴の読み込み中にエラーが発生しました")
            }
        }
    }

    /**
     * 履歴に位置を追加
     */
    private fun addToHistory(position: UserPosition3D) {
        val currentHistory = _positionHistory.value.toMutableList()
        currentHistory.add(position)
        
        // 履歴の最大数を制限（最新1000件）
        if (currentHistory.size > 1000) {
            currentHistory.removeAt(0)
        }
        
        _positionHistory.value = currentHistory
    }

    /**
     * 履歴をクリア
     */
    fun clearHistory() {
        viewModelScope.launch {
            positionRepository.clear3DHistory()
            _positionHistory.value = emptyList()
            Timber.d("3D位置履歴をクリアしました")
        }
    }

    /**
     * 座標データをエクスポート
     */
    fun exportCoordinates() {
        viewModelScope.launch {
            try {
                val positions = _positionHistory.value
                if (positions.isEmpty()) {
                    _error.value = "エクスポートするデータがありません"
                    return@launch
                }

                // CSV形式でエクスポート
                val csvData = buildString {
                    appendLine("Timestamp,X,Y,Z,Accuracy,Confidence,Floor,Source")
                    positions.forEach { position ->
                        appendLine("${position.timestamp},${position.x},${position.y},${position.z},${position.accuracy},${position.confidence},${position.floor},${position.source}")
                    }
                }

                // TODO: ファイルに保存する処理を実装
                Timber.d("3D座標データをエクスポートしました: ${positions.size}件")
            } catch (e: Exception) {
                _error.value = "エクスポート中にエラーが発生しました: ${e.message}"
                Timber.e(e, "エクスポート中にエラーが発生しました")
            }
        }
    }

    /**
     * 更新間隔を設定
     */
    fun setUpdateInterval(intervalMs: Long) {
        updateInterval = intervalMs.coerceIn(100L, 10000L)
        Timber.d("更新間隔を設定しました: ${updateInterval}ms")
    }

    /**
     * エラーをクリア
     */
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopCoordinateUpdates()
        Timber.d("CoordinateDisplayViewModelがクリアされました")
    }
}
