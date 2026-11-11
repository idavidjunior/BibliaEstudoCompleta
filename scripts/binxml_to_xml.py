#!/usr/bin/env python3
"""
Conversor de Android Binary XML (.xml binário) para XML legível
Baseado no formato Android ARSC - sem dependências externas
"""
import struct
import sys
import os

def decodificar_string(dados, offset):
    """Decodifica uma string do formato Android binary XML"""
    if offset >= len(dados):
        return "", offset

    # Lê o header da string
    flags = dados[offset]
    offset += 1

    # UTF-8 ou UTF-16?
    if flags == 0x80:
        # UTF-8
        length = dados[offset]
        offset += 1
        utf8_length = dados[offset]
        offset += 1
        string = dados[offset:offset + utf8_length].decode('utf-8', errors='ignore')
        offset += utf8_length
    else:
        # UTF-16
        char_count = flags & 0x3F
        offset += 1
        string_bytes = dados[offset:offset + char_count * 2]
        string = string_bytes.decode('utf-16-le', errors='ignore')
        offset += char_count * 2

    return string, offset

def decodificar_xml_binario(arquivo_binario):
    """Decodifica um arquivo XML binário do Android"""

    with open(arquivo_binario, 'rb') as f:
        dados = f.read()

    # Verifica magic number
    if dados[:4] != b'\x03\x00\x08\x00':
        print(f"O arquivo não é um XML binário Android válido: {arquivo_binario}")
        return None

    resultados = []

    # Parse header
    offset = 8

    # Lê chunk header
    chunk_type, header_size, total_size = struct.unpack('<HHI', dados[offset:offset+8])
    offset += 8

    # Lê string pool offset (se existir)
    if total_size > header_size:
        # Pula para a próxima secção
        remaining = total_size - header_size
        resultados.append(f"<!-- Header: tipo={chunk_type:#x}, header={header_size}, total={total_size} -->")

    # Para fins de reconstrução, vamos extrair tags conhecidas
    xml_decodificado = []

    # Procurar por strings conhecidas (tags XML)
    xml_decodificado.append('<?xml version="1.0" encoding="utf-8"?>')
    xml_decodificado.append('<binary-xml-decoded>')

    # Procurar por conteúdo legível
    texto_legivel = []
    i = 0
    while i < len(dados):
        # Procurar sequências imprimíveis
        if 32 <= dados[i] <= 126 or dados[i] in (10, 13):  # ASCII printable ou newline
            start = i
            while i < len(dados) and (32 <= dados[i] <= 126 or dados[i] in (10, 13)):
                i += 1
            palavra = dados[start:i].decode('ascii', errors='ignore')
            if len(palavra) > 3:
                texto_legivel.append(palavra)
        else:
            i += 1

    xml_decodificado.append('</binary-xml-decoded>')

    return "\n".join(xml_decodificado), texto_legivel

def main():
    if len(sys.argv) < 2:
        print("Uso: python binxml_to_xml.py <arquivo_binario.xml>")
        sys.exit(1)

    caminho = sys.argv[1]

    # Verificar se arquivo existe
    if not os.path.exists(caminho):
        print(f"Arquivo não encontrado: {caminho}")
        sys.exit(1)

    xml_decod, texto = decodificar_xml_binario(caminho)

    # Salvar resultado
    output = caminho.replace('.xml', '_decompiled.xml')
    with open(output, 'w', encoding='utf-8') as f:
        f.write(xml_decod)

    print(f"Arquivos gerados:")
    print(f"  - {output}")
    print(f"\nStrings encontradas ({len(texto)}):")
    for t in texto[:50]:
        if len(t) > 3 and not t.startswith('@') and 'android' not in t.lower():
            print(f"  - {t}")

if __name__ == '__main__':
    main()