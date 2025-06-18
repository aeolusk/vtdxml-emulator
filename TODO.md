# vtd-xml StAX Emulator TODO

이 파일은 vtd-xml 파서 에뮬레이터 개발을 위한 전체 작업의 상위 TODO 목록입니다.

## 현재 구현 현황
- [x] StAX 기반 XML 파서 어댑터 설계 및 구현 (see: TODO-StaxXmlAdapter.md)
- [x] VTDGen, TagPosition, StaxXmlAdapter 구조 설계 및 구현
- [x] TagPosition에 attributes, textNodes, index 등 정보 저장
- [x] block 단위 MeContext 메모리 관리 (StaxXmlAdapter에서 block 변경 시 MeContext 노드 삭제)
- [x] 주요 VTD-XML API 시그니처 에뮬레이션 (toElement, getText, getAttrVal 등)
- [x] 단위 테스트 및 MeContext 노드 카운트 테스트

## 남은 작업
- [ ] 대용량 XML에서의 성능/메모리 최적화 추가 검증
- [ ] 다양한 XML 구조(네임스페이스, 속성, 텍스트 혼합 등) 테스트
- [ ] 문서/샘플/사용 예제 보강