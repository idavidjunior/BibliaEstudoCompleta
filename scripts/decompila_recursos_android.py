#!/usr/bin/env python3
"""
Decompilador de resources.arsc - Android Resources Binary
Extrai strings, IDs e recursos de APKs Android
"""
import struct
import os
import json

def ler_resources_arsc(caminho_arsc):
    """Lê e decompila um arquivo resources.arsc"""

    with open(caminho_arsc, 'rb') as f:
        dados = f.read()

    resultados = {
        'strings': [],
        'recursos': [],
        'ids': []
    }

    # Parse header
    if len(dados) < 8:
        return resultados

    # Header do arquivo
    header = struct.unpack('<4sHH', dados[:8])
    magic, header_size, type_size, chunk_count = header

    offset = 8

    # Lê string pool (primeiro chunk típico)
    if magic == b'Arsc':
        while offset < len(dados):
            if offset + 8 > len(dados):
                break

            chunk_header = struct.unpack('<HHII', dados[offset:offset+12])
            chunk_type, header_len, chunk_len, res_id = chunk_header

            if chunk_type == 0x0001:  # STRING_POOL
                # Parse string pool
                strings = extrair_string_pool(dados, offset, chunk_len)
                resultados['strings'].extend(strings)

            elif chunk_type == 0x0002:  # TABLE_TYPE
                # Parse resource IDs
                res_ids = extrair_resource_ids(dados, offset, chunk_len)
                resultados['ids'].extend(res_ids)

            offset += chunk_len

    return resultados

def extrair_string_pool(dados, offset_inicial, chunk_len):
    """Extrai strings do string pool"""
    strings = []

    # String pool header (28 bytes)
    if offset_inicial + 28 > len(dados):
        return strings

    sp_header = struct.unpack('<HHHHHIII', dados[offset_inicial:offset_inicial+28])
    sp_header_size, string_count, style_count, flags, strings_start, styles_start = sp_header[1:]

    # Offset para strings
    offset_strings = offset_inicial + strings_start

    # Lê offsets das strings
    offsets = []
    for i in range(string_count):
        if offset_strings + i*4 > offset_inicial + chunk_len:
            break
        string_offset = struct.unpack('<I', dados[offset_strings + i*4:offset_strings + i*4 + 4])[0]
        offsets.append(string_offset)

    # Lê cada string
    string_data_start = offset_strings + string_count * 4

    for i, string_offset in enumerate(offsets):
        string_pos = string_data_start + string_offset

        if string_pos + 1 > len(dados):
            continue

        # String UTF-8 ou UTF-16
        length_raw = dados[string_pos]
        string_length = length_raw if (length_raw & 0x80) == 0 else ((length_raw & 0x7F) << 1) | (length_raw + 1 if length_raw + 1 < len(dados) else 0)

        # Lê string
        try:
            if (length_raw & 0x80) == 0:
                # UTF-8
                string_bytes = dados[string_pos + 1:string_pos + 1 + length_raw]
                string_val = string_bytes.decode('utf-8', errors='ignore')
            else:
                # UTF-16
                char_count = length_raw & 0x7F
                utf16_bytes = dados[string_pos + 1:string_pos + 1 + char_count*2]
                string_val = utf16_bytes.decode('utf-16-le', errors='ignore')

            if string_val and string_val.isprintable():
                strings.append(string_val)
        except:
            pass

    return strings

def extrair_resource_ids(dados, offset_inicial, chunk_len):
    """Extrai IDs de recursos"""
    ids = []

    # Key map header
    if offset_inicial + 12 > len(dados):
        return ids

    try:
        key_map_count = struct.unpack('<I', dados[offset_inicial + 8:offset_inicial + 12])[0]

        for i in range(key_map_count):
            id_offset = offset_inicial + 12 + i * 4
            if id_offset + 4 > len(dados):
                break
            res_id = struct.unpack('<I', dados[id_offset:id_offset+4])[0]
            if res_id:
                ids.append(res_id)
    except:
        pass

    return ids

def extrair_strings_layout(layout_xml):
    """Extrai strings visíveis de um layout XML"""
    strings = []

    # Padrões comuns de strings no Android
    padroes = [
        r'android:text="([^"]*)"',
        r'android:title="([^"]*)"',
        r'android:hint="([^"]*)"',
        r'android:name="([^"]*)"',
        r'android:contentDescription="([^"]*)"',
        r'droidText="([^"]*)"'
    ]

    for padrao in padroes:
        import re
        matches = re.findall(padrao, layout_xml)
        strings.extend(matches)

    return strings

def main():
    """Função principal"""
    apk_path = 'bin/biblia_source/base.apk'
    output_path = 'bin/biblia_source/interface_fiel_completa/res/values/strings.xml'

    # Verificar se APK existe
    if not os.path.exists(apk_path):
        print(f"APK não encontrado: {apk_path}")
        return

    # Extrair resources.arsc do APK
    import zipfile
    try:
        with zipfile.ZipFile(apk_path, 'r') as zf:
            if 'resources.arsc' in zf.namelist():
                zf.extract('resources.arsc', 'bin/biblia_source/')
                print("✓ resources.arsc extraído")
            else:
                print("resources.arsc não encontrado no APK")
                return
    except Exception as e:
        print(f"Erro ao extrair: {e}")
        return

    # Decompilar
    arsc_path = 'bin/biblia_source/resources.arsc'
    recursos = ler_resources_arsc(arsc_path)

    # Gerar strings.xml
    todas_strings = list(set(recursos['strings']))

    # Filtrar strings internas (não UI)
    ui_strings = [s for s in todas_strings
                  if len(s) > 3
                  and not s.startswith('@')
                  and not s.startswith('Android')
                  and 'android' not in s.lower()]

    # Escrever strings.xml
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    with open(output_path, 'w', encoding='utf-8') as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n')
        f.write('<resources>\n')
        for s in sorted(ui_strings)[:500]:  # Limitar a 500 strings mais relevantes
            s_escaped = s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
            if s_escaped and not s_escaped.startswith('['):
                f.write(f'    <string name="auto_{len(s)}">{s_escaped}</string>\n')
        f.write('</resources>\n')

    print(f"✓ strings.xml gerado: {len(ui_strings)} strings encontradas")
    print(f"✅ Decompilação concluída")

if __name__ == '__main__':
    main()