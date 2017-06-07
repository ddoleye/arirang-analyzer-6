arirang-analyzer 6
=================

soomyung 님이 만드신 [arirang-analyzer-6](https://github.com/soomyung/arirang-analyzer-6) 에서 
[Managed Resources](https://cwiki.apache.org/confluence/display/solr/Managed+Resources)를 지원하는 버전입니다.  

ManagedKoreanFilterFactory를 사용하여 단어사전(total.dic, extension.dic), 복합명사사전(compounds.dic), 복합명사분해사전(uncompounds.dic)을 추가할 수 있습니다.

이 버전은 사전기능을 수정한 [arirang.morph](https://github.com/ddoleye/arirang.morph) 를 사용합니다.

managed 버전은 ManagedKoreanFilterFactory, ManagedHanjaMappingFilterFactory, ManagedWordSegmentFilterFactory 세가지가 있습니다.

스키마를 아래와 같이 설정하여 사용합니다.

    <fieldType name="managed_ko" class="solr.TextField">
    <analyzer type="index">
      <tokenizer class="org.apache.lucene.analysis.ko.KoreanTokenizerFactory"/>
      <!-- ... -->
      <filter class="org.apache.lucene.analysis.ko.managed.ManagedKoreanFilterFactory" hasOrigin="true" hasCNoun="true"  bigrammable="false" queryMode="false" managed="ko" />
      <filter class="org.apache.lucene.analysis.ko.managed.ManagedHanjaMappingFilterFactory" managed="ko" />
      <!-- ... -->
    </analyzer>
    <analyzer type="query">
      <tokenizer class="org.apache.lucene.analysis.ko.KoreanTokenizerFactory"/>
      <!-- ... -->
      <filter class="org.apache.lucene.analysis.ko.managed.ManagedKoreanFilterFactory" hasOrigin="true" hasCNoun="true"  bigrammable="false" queryMode="true" managed="ko" />
      <filter class="org.apache.lucene.analysis.ko.managed.ManagedWordSegmentFilterFactory" hasOrigin="true" managed="ko"/>
      <filter class="org.apache.lucene.analysis.ko.managed.ManagedHanjaMappingFilterFactory" managed="ko" />
      <!-- ... -->
    </analyzer>
    </fieldType>

ManagedKoreanFilterFactory의 hasOrigin, hasCNoun, bigrammable, queryMode 는 기존과 동일하고 추가된 속성은 다음과 같습니다

**managed**

사전의 관리되는 이름을 지정합니다. 각 필터에서 managed 이름이 다를 경우는 다른 사전을 사용하게 됩니다.




사전 항목 추가
-------------------

사전 내용을 확인합니다.

    curl -X GET http://localhost:8983/solr/example/schema/analysis/arirang/ko
    <응답>
    {
      "responseHeader":{
        "status":0,
        "QTime":1},
      "resources":{
        "initArgs":{"emptyWords":false},
        "initializedOn":"2017-06-07T02:06:02.994Z",
        "updatedSinceInit":"2017-06-07T02:19:27.639Z",
        "managedMap":{
          "words":[],
          "compounds":[],
          "uncompounds":[]}}}

initArgs 에서 emptyWords:false 는 arirang.morph 에 포함된 기본단어 사전을 사용한다는 뜻입니다. 
기본단어 사전을 사용하지 않도록 설정하는 경우는 단어사전(total.dic, extension.dic), 복합명사 사전(compounds.dic), 복합명사 분해 사전(extension.dic)을 로드하지 않습니다.

기본 사전을 로드하지 않도록 하려면 다음을 실행합니다.

    curl -X POST \
       http://localhost:8983/solr/example/schema/analysis/arirang/ko \
      -H 'content-type: application/json' \
      -d '{ initArgs: {emptyWords: true} }'

사전 내용 설정
  
    curl -X POST http://localhost:8983/solr/example/schema/analysis/arirang/ko \
      -H 'content-type: application/json' \
      -d '{words:["#WORD,NVZDBIPSCC", \
                  "가,110000000X", \
                  "가가,100000000X", \
                  "가가호호,101000000X", ... ], \
           compounds:["아르고넷:아르고,넷:0000",...] \
          }'
    <응답>
    {
     "responseHeader":{
      "status":0,
      "QTime":2}}
      
설정된 사전 내용을 확인합니다.

    curl -X GET http://localhost:8983/solr/example/schema/analysis/arirang/ko
    <응답>
    {
      "responseHeader": {
        "status": 0,
        "QTime": 1
      },
      "resources": {
        "initArgs": {
          "emptyWords": true
        },
        "initializedOn": "2017-06-07T02:06:02.994Z",
        "updatedSinceInit": "2017-06-07T02:33:02.671Z",
        "managedMap": {
          "words": [
            "#WORD,NVZDBIPSCC",
            "가,110000000X",
            "가가,100000000X",
            "가가호호,101000000X", ...],
          "compounds": [ ... ],
          "uncompounds": [ ... ]}
          }
       }
       

사전 삭제
-------------------

POST 또는 PUT 의 경우 사전에 데이터를 추가합니다. 사전의 내용을 삭제할 경우는 words/compounds/uncompounds 단위로 전체를 삭제합니다.

    curl -X DELETE \
      http://localhost:8983/solr/example/schema/analysis/arirang/ko/words \

    curl -X DELETE \
      http://localhost:8983/solr/example/schema/analysis/arirang/ko/compounds \

    curl -X DELETE \
      http://localhost:8983/solr/example/schema/analysis/arirang/ko/uncompounds \


사전 적용
-------------------

추가한 내용을 Filter에 적용하기 위해서는 core를 reload 해야 합니다.(이것은 Managed Resources 특성입니다)

