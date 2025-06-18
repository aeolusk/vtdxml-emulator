# TODO-StaxXmlAdapter

StaxXmlAdapter 개발 및 개선 작업 목록

- [x] 500MB 이상 XML 파일도 1MB 단위로 스트리밍 처리
- [x] StAX 기반 XML 파서 구현
- [x] TagPosition에 속성, 텍스트, 인덱스 등 정보 저장
- [x] parent/child 트리 구조 및 block 단위 MeContext 메모리 관리
- [x] block 변경 시 MeContext 노드 삭제 로직 구현
- [x] position 정보 기반 탐색/조회 메서드 구현
- [x] 단위 테스트 작성

## 남은 작업
- [ ] 다양한 XML 구조(네임스페이스, 속성, 텍스트 혼합 등) 테스트
- [ ] 문서/샘플/사용 예제 보강