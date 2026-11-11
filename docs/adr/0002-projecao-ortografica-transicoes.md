# ADR 0002: Projeção ortográfica nas transições OpenGL

## Status

Aceito.

## Contexto

As transições de página usam OpenGL ES 2.0. Com a projeção de perspectiva
`frustumM`, a página renderizada tinha a proporção distorcida e não ocupava a
tela inteira. A página capturada tem a mesma proporção do aparelho, por
exemplo 1080 por 2400.

## Decisão

Usar a projeção ortográfica `orthoM(-1, 1, -1, 1, 2, 8)` e remover a
multiplicação de `pos.x` pela proporção antiga no shader.

## Consequências

- A página preenche a tela inteira, sem distorção.
- Os quatro efeitos de transição validados no aparelho passaram a funcionar.
- A proporção da página deixou de precisar ser transportada até o shader.
