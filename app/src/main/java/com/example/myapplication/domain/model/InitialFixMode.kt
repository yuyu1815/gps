package com.example.myapplication.domain.model

/**
 * 初期位置の確定方法を指定するモード。
 */
enum class InitialFixMode {
    /** Wi‑Fi等の推定が得られたら自動で初期位置を確定する */
    AUTO,
    /** ユーザーの手動操作で初期位置を確定する（自動では確定しない） */
    MANUAL,
    /** 一定時間で暫定の原点(0,0)を設定し、後から上書き可能とする */
    TEMPORARY
}


