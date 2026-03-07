# JSON Template Fixed-Length Parser

Lightweight template-based parser for converting JSON and fixed-length
messages.

JSON Template 기반으로 **Fixed-Length 메시지와 JSON 간 변환**을 지원하는
간단한 파서입니다.

레거시 시스템이나 전문 메시지(고정 길이 포맷)를 JSON 구조와 매핑하기
위한 용도로 만들었습니다.

    JSON → Fixed-Length
    Fixed-Length → JSON

Template 기반으로 동작하기 때문에 메시지 구조 변경 시 코드 수정 없이
**Template만 수정하면 됩니다.**

------------------------------------------------------------------------

# Features

-   JSON Template 기반 메시지 매핑
-   Fixed-Length ↔ JSON 양방향 변환
-   중첩 Object 구조 지원
-   List 반복 구조 지원
-   Primitive Array 지원
-   List Count 계산 (`LN`)
-   JSON 문자열 필드 (`JS`, `JO`)

지원 타입

Type   Description
  ------ -----------------
O      Object
L      List
A      Primitive Array
S      String
N      Number
LN     List Count
JS     JSON String
JO     JSON Object

------------------------------------------------------------------------

# Project Structure

    parse
    ├── config
    ├── enums
    ├── service
    │   └── ParseService
    ├── util
    └── resources
        └── json

Template JSON 파일은 다음 위치에 둡니다.

    src/main/resources/json/

------------------------------------------------------------------------

# Usage

## Template Example

`sample.json`

``` json
{
  "type": "O",
  "name": {
    "type": "S",
    "length": "10"
  },
  "age": {
    "type": "N",
    "length": "3"
  }
}
```

------------------------------------------------------------------------

## JSON → Fixed-Length

``` java
Map<String, Object> data = new HashMap<>();
data.put("name", "modekoo");
data.put("age", 38);

String flat = parseService.jsonToFlat(data, "sample", "UTF-8");
```

Result

    modekoo   038

------------------------------------------------------------------------

## Fixed-Length → JSON

``` java
Map<String, Object> result =
        parseService.flatToJson(flat, "sample", "UTF-8");
```

Result

``` json
{
  "name": "modekoo",
  "age": "38"
}
```

------------------------------------------------------------------------

# Template Specification

## Object

``` json
{
  "type": "O",
  "field": { ... }
}
```

------------------------------------------------------------------------

## String

``` json
{
  "type": "S",
  "length": "10"
}
```

Right padded with spaces.

------------------------------------------------------------------------

## Number

``` json
{
  "type": "N",
  "length": "5"
}
```

Left padded with `0`.

------------------------------------------------------------------------

## List

``` json
{
  "type": "L",
  "length": "2",
  "field": { ... }
}
```

------------------------------------------------------------------------

## Primitive Array

``` json
{
  "type": "A",
  "length": "3",
  "tag": {
    "type": "S",
    "length": "5"
  }
}
```

------------------------------------------------------------------------

## List Count

``` json
{
  "type": "LN",
  "length": "3",
  "value": "items"
}
```

List size will be written automatically.

------------------------------------------------------------------------

## JSON Object

``` json
{
  "type": "JO",
  "length": "30"
}
```

Serialized as JSON string.

------------------------------------------------------------------------

# Tests

테스트 케이스는 다음을 검증합니다.

-   기본 변환
-   깊은 Object 구조
-   누락 필드 처리
-   List 반복
-   Primitive Array
-   List Count (`LN`)

테스트 실행

    ./gradlew test

------------------------------------------------------------------------

# Example

Input JSON

``` json
{
  "a": {
    "b": {
      "c": {
        "d": "HELLO",
        "e": 12
      },
      "f": "ABCD"
    },
    "g": "ZZ"
  },
  "h": "END"
}
```

Converted Fixed-Length

    HELLO012ABCDZZEND

------------------------------------------------------------------------

# Why

레거시 시스템이나 전문 메시지 포맷을 다루다 보면\
고정 길이 메시지 구조를 코드로 직접 파싱하는 경우가 많습니다.

뎁스 한계나 가시성 문제를 줄이기 위해 재귀 기반으로 구현했습니다.

이 프로젝트는 메시지 구조를 JSON Template으로 정의하고 재귀적으로
파싱하는 방식으로\
구조 변경 시 코드 수정 없이 Template만으로 처리할 수 있도록 만든
실험적인 구현입니다.

------------------------------------------------------------------------

# License

MIT