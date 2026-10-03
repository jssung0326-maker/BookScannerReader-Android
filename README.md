# BookScannerReader-Android v1.0-alpha

iPhone용 BookScannerReader v1.0의 기능 구조를 Android로 이식한 첫 테스트 버전입니다.

## 목표 기기
- Samsung Galaxy Note8
- minSdk 23
- Kotlin + Jetpack Compose + CameraX + ML Kit

## 현재 포함
- 1페이지 / 펼친 2페이지 촬영
- 2페이지 좌우 분리 및 분리 위치 조절
- 기준 출력 크기 정규화
- 품질 점수
- 중복 페이지 감지
- 이어 스캔
- 한글 OCR
- 이미지 PDF 생성
- OCR 본문 검색
- 북마크 / 메모
- Android TTS 읽어주기
- 책별 로컬 저장

## iOS v1.0과 차이
- iOS VisionKit/Vision/PDFKit/TTS는 Android CameraX/ML Kit/PdfDocument/TTS로 교체했습니다.
- 제본선 자동검출, 고급 원근 보정, 곡면 보정, 페이지 안정 자동촬영은 실제 Note8 촬영 샘플로 튜닝한 뒤 다음 버전에서 강화합니다.
- 검색 가능한 PDF의 invisible OCR text layer는 현재 앱 내부 OCR 검색으로 대체합니다.

## 실행
1. Windows에 Android Studio 설치
2. 이 폴더를 Open
3. Gradle Sync 완료
4. Note8: 설정 > 휴대전화 정보 > 소프트웨어 정보 > 빌드번호 7회 탭
5. 개발자 옵션 > USB 디버깅 ON
6. USB 연결 후 PC 허용
7. Android Studio 상단에서 Note8 선택
8. Run ▶

## GitHub
새 저장소 이름 권장: `BookScannerReader-Android`
기존 iOS `BookScannerReader` 저장소는 그대로 유지하세요.
