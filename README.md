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
       extension="extension" compounds="compounds" uncompounds="uncompounds" /><!-- 사전 콘텐츠 추가 가능 -->
      ...
    </analyzer>
    <analyzer type="query">
      ...
      <filter class="org.apache.lucene.analysis.ko.managed.ManagedKoreanFilterFactory"
       hasOrigin="true" hasCNoun="true" bigrammable="false" queryMode="true" 
       extension="extension" compounds="compounds" uncompounds="uncompounds" /><!-- 사전 콘텐츠 추가 가능 -->
      ...
    </analyzer>
    </fieldType>

ManagedResource 사용
-------------------

추가 사전 내용을 확인합니다.

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

추가 사전 변경
  
    curl -X POST http://localhost:8983/solr/example/schema/analysis/arirang/compounds/compounds \
      -H 'content-type: application/json' \
      -d '[ "아르고넷:아르고,넷:0000" ]'
    <응답>
    {
     "responseHeader":{
      "status":0,
      "QTime":2}}
      

추가된 사전 내용을 확인합니다.

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

