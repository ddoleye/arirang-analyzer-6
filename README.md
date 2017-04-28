arirang-analyzer 6
=================

soomyung 님이 만드신 [arirang-analyzer-6](https://github.com/soomyung/arirang-analyzer-6) 에서 [Managed Resources](https://cwiki.apache.org/confluence/display/solr/Managed+Resources)를 지원하는 버전입니다.  

ManagedKoreanFilterFactory를 사용하여 확장사전(extension.dic), 복합명사사전(compounds.dic), 복합명사분해사전(uncompounds.dic)을 추가할 수 있습니다.

이 버전은 사전기능을 수정한 [arirang.morph](https://github.com/ddoleye/arirang.morph) 를 사용합니다.

스키마를 아래와 같이 설정하여 사용합니다.

    <fieldType name="managed_ko" class="solr.TextField">
    <analyzer type="index">
      ...
      <filter class="org.apache.lucene.analysis.ko.managed.ManagedKoreanFilterFactory"
       hasOrigin="true" hasCNoun="true"  bigrammable="false" queryMode="false"
       emptyWords="false" words="ko" compounds="compounds" uncompounds="uncompounds" /><!-- 사전 콘텐츠 추가 가능 -->
      ...
    </analyzer>
    <analyzer type="query">
      ...
      <filter class="org.apache.lucene.analysis.ko.managed.ManagedKoreanFilterFactory"
       hasOrigin="true" hasCNoun="true" bigrammable="false" queryMode="true" 
       emptyWords="false" words="ko" compounds="compounds" uncompounds="uncompounds" /><!-- 사전 콘텐츠 추가 가능 -->
      ...
    </analyzer>
    </fieldType>

hasOrigin, hasCNoun, bigrammable, queryMode 는 기존과 동일하고 추가된 속성은 다음과 같습니다

**emptyWords**

arirang 기본 단어 사전을 로드할지를 결정합니다. emptyWords=true 일 경우는 arirang 기본 사전인 total.dic, extension.dic, compounds.dic, uncompounds.dic 을 사용하지 않고 조사, 어미 등의 사전만 로드합니다.

**words**

단어 사전의 관리되는 리소스를 지정합니다. words="ko" 일 경우는 /schema/analysis/arirang/words/ko 에 매핑됩니다.

**compounds**

복합명사 사전의 관리되는 리소스를 지정합니다. compounds="compounds" 일 경우는 /schema/analysis/arirang/compounds/compounds 에 매핑됩니다.

**uncompounds**

복합명사 분해 사전의 관리되는 리소스를 지정합니다. uncompounds="uncompounds" 일 경우는 /schema/analysis/arirang/uncompounds/uncompounds 에 매핑됩니다.


사전 항목 추가
-------------------

사전 내용을 확인합니다.

    curl -X GET http://localhost:8983/solr/example/schema/analysis/arirang/compounds/compounds
    <응답>
    {
     "responseHeader":{
     "status":0,
     "QTime":1},
     "contents":{
     "initArgs":{},
     "initializedOn":"2017-04-27T09:55:23.660Z",
     "updatedSinceInit":"2017-04-27T09:56:10.099Z",
     "managedList":[]}}

사전 내용 설정
  
    curl -X POST http://localhost:8983/solr/example/schema/analysis/arirang/compounds/compounds \
      -H 'content-type: application/json' \
      -d '[ "아르고넷:아르고,넷:0000" ]'
    <응답>
    {
     "responseHeader":{
      "status":0,
      "QTime":2}}
      

설정된 사전 내용을 확인합니다.

    curl -X GET http://localhost:8983/solr/example/schema/analysis/arirang/compounds/compounds
    <응답>
    {
     "responseHeader":{
     "status":0,
     "QTime":1},
     "contents":{
     "initArgs":{},
     "initializedOn":"2017-04-27T09:55:23.660Z",
     "updatedSinceInit":"2017-04-27T09:56:10.099Z",
     "managedList":["아르고넷:아르고,넷:0000"]}}

추가한 내용을 Filter에 적용하기 위해서는 core를 reload 해야 합니다.(이것은 Managed Resources 특성입니다)

