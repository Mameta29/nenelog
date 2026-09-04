// Week 1で実機合格したiOS L2音声エンジン。SpeechSpikeViewは診断UIとして残し、
// IosNursingVoiceEngine本体は通常のComposeホーム画面からも使用する。
// 検証項目:
//   (a) SFSpeechRecognizer の ja_JP オンデバイス対応(supportsOnDeviceRecognition)
//   (b) iOS 26+ の SpeechTranscriber / SpeechAnalyzer の supportedLocales に ja が含まれるか
//   (c) AVAudioSession(playAndRecord + voiceChat + duckOthers)下での連続認識と
//       「speak 中はマイク停止 → 終了後300msで再開」ループ(docs/04)
//
// app/iosApp の iosApp ターゲットへ直接追加済み。Speech タブから実行し、
// 結果は research/spike-results.md へ転記する。

import SwiftUI
import Speech
import AVFoundation
import ComposeApp

/**
 * The audio tap must stay installed while the app is backgrounded. Only swap the
 * recognition request so TTS audio is discarded instead of being recognized.
 */
private final class SpikeAudioRequestSink: @unchecked Sendable {
    private let lock = NSLock()
    private var request: SFSpeechAudioBufferRecognitionRequest?

    func replace(with request: SFSpeechAudioBufferRecognitionRequest?) {
        lock.lock()
        self.request = request
        lock.unlock()
    }

    func append(_ buffer: AVAudioPCMBuffer) {
        lock.lock()
        let currentRequest = request
        lock.unlock()
        currentRequest?.append(buffer)
    }
}

struct SpeechSpikeView: View {
    @StateObject private var engine = IosNursingVoiceEngine()
    @State private var useJapanese = true

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("iOS Speech Spike").font(.title2).bold()
            Toggle("日本語 (ja-JP)", isOn: $useJapanese)
            VStack(alignment: .leading, spacing: 8) {
                Button("能力チェック") { engine.checkCapabilities(japanese: useJapanese) }
                HStack {
                    Button("連続認識開始") { engine.startLoop(japanese: useJapanese) }
                    Button("停止") { engine.stop() }
                }
            }.buttonStyle(.borderedProminent)
            Text(engine.isRunning ? "状態: 認識中" : "状態: 停止中")
                .font(.caption)
                .foregroundStyle(engine.isRunning ? .green : .secondary)
            List(engine.log.indices.reversed(), id: \.self) { index in
                Text(engine.log[index]).font(.caption)
            }
        }
        .padding()
    }
}

@MainActor
final class IosNursingVoiceEngine: NSObject, ObservableObject {
    @Published var log: [String] = []
    @Published var isRunning = false
    var onUiStateChange: ((String, String?) -> Void)?

    private let audioEngine = AVAudioEngine()
    private var recognizer: SFSpeechRecognizer?
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let synthesizer = AVSpeechSynthesizer()
    private var running = false
    private var currentLocale = "ja-JP"
    private var recognitionGeneration = 0
    private var tapInstalled = false
    private var pendingRestart: DispatchWorkItem?
    private var pendingEndpoint: DispatchWorkItem?
    private var latestTranscription = ""
    private let requestSink = SpikeAudioRequestSink()
    private var audioStartFailureCount = 0
    private var endSessionAfterSpeech = false

    func add(_ line: String) { log.append(line); print("[Spike] \(line)") }

    private func publishUiState(_ stateCode: String, transcript: String? = nil) {
        onUiStateChange?(stateCode, transcript)
    }

    func checkCapabilities(japanese: Bool) {
        let localeId = japanese ? "ja-JP" : "en-US"

        // (a) SFSpeechRecognizer
        let supported = SFSpeechRecognizer.supportedLocales().map(\.identifier).sorted()
        add("SFSpeechRecognizer.supportedLocales: \(supported.count)件, ja系=\(supported.filter { $0.hasPrefix("ja") })")
        if let r = SFSpeechRecognizer(locale: Locale(identifier: localeId)) {
            add("\(localeId): available=\(r.isAvailable) onDevice=\(r.supportsOnDeviceRecognition)")
        } else {
            add("\(localeId): SFSpeechRecognizer 生成不可")
        }

        // (b) iOS 26+ SpeechTranscriber(docs/03: 39ロケールに ja_JP が含まれる想定を実機確認)
        if #available(iOS 26.0, *) {
            Task { @MainActor in
                // NOTE: 実装時に SpeechTranscriber.supportedLocales の正確なAPI名をXcodeで確認すること
                self.add("iOS 26+: SpeechAnalyzer/SpeechTranscriber APIあり — supportedLocales を確認せよ")
            }
        } else {
            add("iOS 26未満: SpeechTranscriber なし → SFSpeechRecognizer 経路")
        }
    }

    func startLoop(japanese: Bool) {
        startLoop(localeIdentifier: japanese ? "ja-JP" : "en-US")
    }

    func startLoop(localeIdentifier: String) {
        guard !running else { return }
        currentLocale = localeIdentifier.hasPrefix("ja") ? "ja-JP" : "en-US"
        publishUiState("waiting")
        SFSpeechRecognizer.requestAuthorization { status in
            Task { @MainActor in
                self.add("speech auth=\(status.rawValue)")
                guard status == .authorized else {
                    self.publishUiState("failure")
                    return
                }
                self.requestMicrophonePermission()
            }
        }
    }

    func stop() {
        guard running || isRunning || audioEngine.isRunning || synthesizer.isSpeaking else { return }
        running = false
        isRunning = false
        publishUiState("waiting")
        pendingRestart?.cancel()
        pendingRestart = nil
        invalidateRecognition(keepAudioIO: false)
        audioStartFailureCount = 0
        endSessionAfterSpeech = false
        synthesizer.stopSpeaking(at: .immediate)
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        add("stopped")
    }

    private func requestMicrophonePermission() {
        let handlePermission: @Sendable (Bool) -> Void = { [weak self] granted in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.add("mic granted=\(granted)")
                guard granted else {
                    self.publishUiState("failure")
                    return
                }
                self.running = true
                self.isRunning = true
                self.beginRecognition()
            }
        }

        if #available(iOS 17.0, *) {
            AVAudioApplication.requestRecordPermission(completionHandler: handlePermission)
        } else {
            AVAudioSession.sharedInstance().requestRecordPermission(handlePermission)
        }
    }

    private func beginRecognition() {
        guard running else { return }
        pendingRestart?.cancel()
        pendingRestart = nil
        invalidateRecognition()
        let generation = recognitionGeneration

        do {
            // docs/04: playAndRecord + voiceChat + duckOthers。バックグラウンド継続は
            // Capabilities > Background Modes > Audio を ON にして画面オフでも試す
            // Once backgrounded, iOS does not allow a stopped recording I/O unit to
            // start again (`!rec`). Keep the I/O unit alive across recognition/TTS
            // cycles, and only detach the active recognition request while speaking.
            if !audioEngine.isRunning {
                let session = AVAudioSession.sharedInstance()
                try session.setCategory(
                    .playAndRecord,
                    mode: .voiceChat,
                    options: [.duckOthers, .defaultToSpeaker]
                )
                try session.setActive(true, options: .notifyOthersOnDeactivation)
            }

            recognizer = SFSpeechRecognizer(locale: Locale(identifier: currentLocale))
            let req = SFSpeechAudioBufferRecognitionRequest()
            req.requiresOnDeviceRecognition = recognizer?.supportsOnDeviceRecognition ?? false
            req.shouldReportPartialResults = true
            req.taskHint = .confirmation
            request = req
            requestSink.replace(with: req)
            add("recognition start (onDevice=\(req.requiresOnDeviceRecognition))")
            publishUiState("listening")

            let input = audioEngine.inputNode
            if !tapInstalled {
                let format = input.outputFormat(forBus: 0)
                let sink = requestSink
                input.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, _ in
                    sink.append(buffer)
                }
                tapInstalled = true
            }
            if !audioEngine.isRunning {
                audioEngine.prepare()
                try audioEngine.start()
            }
            audioStartFailureCount = 0

            recognitionTask = recognizer?.recognitionTask(with: req) { [weak self] result, error in
                Task { @MainActor in
                    guard let self,
                          self.running,
                          self.recognitionGeneration == generation else { return }

                    if let result {
                        let text = result.bestTranscription.formattedString
                            .trimmingCharacters(in: .whitespacesAndNewlines)
                        if !text.isEmpty, text != self.latestTranscription {
                            self.latestTranscription = text
                            self.add("partial: \(text)")
                        }
                        if result.isFinal {
                            self.commitTranscription(text, source: "recognizer")
                            return
                        }
                        if !text.isEmpty {
                            // Live/on-device recognition may not emit isFinal while audio keeps
                            // streaming. Treat 800ms without a changed hypothesis as the endpoint.
                            self.scheduleEndpoint(text, generation: generation)
                        }
                    }
                    if let error {
                        if !self.latestTranscription.isEmpty {
                            self.commitTranscription(self.latestTranscription, source: "error endpoint")
                            return
                        }
                        self.add("error: \(error.localizedDescription)")
                        self.publishUiState("failure")
                        self.restartSoon(delayMs: 500)
                    }
                }
            }
        } catch {
            audioStartFailureCount += 1
            let retryDelayMs = min(5_000, 750 * audioStartFailureCount)
            add("audio session error: \(error) — retry in \(retryDelayMs)ms")
            publishUiState("failure")
            restartSoon(delayMs: retryDelayMs)
        }
    }

    private func scheduleEndpoint(_ text: String, generation: Int) {
        pendingEndpoint?.cancel()
        let workItem = DispatchWorkItem { [weak self] in
            guard let self,
                  self.running,
                  self.recognitionGeneration == generation else { return }
            self.pendingEndpoint = nil
            self.commitTranscription(text, source: "silence")
        }
        pendingEndpoint = workItem
        DispatchQueue.main.asyncAfter(
            deadline: .now() + .milliseconds(800),
            execute: workItem
        )
    }

    private func commitTranscription(_ text: String, source: String) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard running, !trimmed.isEmpty else { return }
        pendingEndpoint?.cancel()
        pendingEndpoint = nil
        add("FINAL(\(source)): \(trimmed)")
        publishUiState("recognized", transcript: trimmed)
        let bridge = AppIntentDomainBridge()
        let epochMillis = Int64(Date().timeIntervalSince1970 * 1_000)
        guard let response = bridge.handleVoiceCommandResponse(
            transcript: trimmed,
            epochMillis: epochMillis,
            localeCode: currentLocale
        ) else {
            add("ignored: not a standalone Nenelog command")
            publishUiState("failure", transcript: trimmed)
            invalidateRecognition()
            restartSoon(delayMs: 250)
            return
        }
        let shouldEndSession = bridge.isStopVoiceCommand(
            transcript: trimmed,
            localeCode: currentLocale
        )
        speakBack(response, endSessionAfterSpeaking: shouldEndSession)
    }

    /** docs/04: speak 中はマイク停止 → 終了後300msで再開(エコー対策・AEC非依存) */
    private func speakBack(_ text: String, endSessionAfterSpeaking: Bool) {
        invalidateRecognition()
        endSessionAfterSpeech = endSessionAfterSpeaking
        publishUiState("responding")

        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: currentLocale)
        synthesizer.delegate = self
        synthesizer.speak(utterance)
    }

    fileprivate func restartSoon(delayMs: Int) {
        guard running else { return }
        guard pendingRestart == nil else { return }

        let workItem = DispatchWorkItem { [weak self] in
            guard let self else { return }
            self.pendingRestart = nil
            guard self.running else { return }
            self.publishUiState("waiting")
            self.beginRecognition()
        }
        pendingRestart = workItem
        DispatchQueue.main.asyncAfter(
            deadline: .now() + .milliseconds(delayMs),
            execute: workItem
        )
    }

    /** 古い認識タスクのコールバックを generation で無効化し、tapを必ず1つに保つ。 */
    private func invalidateRecognition(keepAudioIO: Bool = true) {
        recognitionGeneration += 1
        pendingEndpoint?.cancel()
        pendingEndpoint = nil
        latestTranscription = ""
        recognitionTask?.cancel()
        recognitionTask = nil
        requestSink.replace(with: nil)
        request?.endAudio()
        request = nil
        if !keepAudioIO {
            if audioEngine.isRunning { audioEngine.stop() }
            if tapInstalled {
                audioEngine.inputNode.removeTap(onBus: 0)
                tapInstalled = false
            }
            audioEngine.reset()
        }
    }
}

extension IosNursingVoiceEngine: AVSpeechSynthesizerDelegate {
    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        Task { @MainActor in
            if self.endSessionAfterSpeech {
                self.add("speak done → session complete")
                self.publishUiState("waiting")
                self.stop()
            } else {
                self.add("speak done → 300ms後に認識再開")
                self.publishUiState("waiting")
                self.restartSoon(delayMs: 300)
            }
        }
    }
}
