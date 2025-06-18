# TODO-VTDGen

VTDGen 클래스 및 관련 구조 개발을 위한 세부 작업 목록입니다.

- [x] 생성자 구현
- [x] parseFile 메서드 StAX 연동
- [x] 예외 처리 및 반환값 설계
- [x] VTDNav와의 연동 메서드 추가
- [x] getChildren() 메서드 구현 (TagPositionManager의 루트 노드에서 자식 노드 반환)
- [x] TagPosition에 attributes, textNodes, index 등 정보 저장
- [x] block 단위 MeContext 메모리 관리 (StaxXmlAdapter에서 block 변경 시 MeContext 노드 삭제)
- [x] VTD-XML 스타일의 커서 기반 탐색(toElement 등) 구현
- [x] 단위 테스트 작성 (MeContext 개수 카운트 등)

## 남은 작업
- [ ] 대용량 XML에서의 성능/메모리 최적화 추가 검증
- [ ] 다양한 XML 구조(네임스페이스, 속성, 텍스트 혼합 등) 테스트
- [ ] 문서/샘플/사용 예제 보강