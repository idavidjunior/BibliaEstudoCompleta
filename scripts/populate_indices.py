#!/usr/bin/env python3
"""
Script para popular o banco indices.db com dados de estudo bíblico.
Executa: python scripts/populate_indices.py
"""
import sqlite3
import os

DB_PATH = os.path.join(os.path.dirname(__file__), "..", "assets", "databases", "indices.db")

TOPICS = [
    ("Gênesis e Criação", "O início de tudo: a criação do mundo, Adão e Eva, o Dilúvio e os patriarcas", "Fundamentos"),
    ("Êxodo e Libertação", "A saída do Egito, as pragas, a travessia do Mar Vermelho e os 10 mandamentos", "Fundamentos"),
    ("Levítico e Adoração", "Leis cerimoniais, sacrifícios, pureza e o tabernáculo", "Leis e Cultura"),
    ("Deuteronômio e Aliança", "Renovação da aliança, bênçãos e maldições, a Lei de Moisés", "Leis e Cultura"),
    ("Conquista de Canaã", "Josué, as muralhas de Jericó, a partilha da terra prometida", "História"),
    ("Juízes e Período de Instabilidade", "Débora, Gideão, Sansão e o ciclo de pecado e libertação", "História"),
    ("Reino Unido", "Saul, Davi e Salomão: a unificação de Israel e a construção do templo", "História"),
    ("Reino Dividido", "Israel e Judá separados, profetas e reis, idólatra e reformas", "História"),
    ("Exílio Babilônico", "A destruição de Jerusalém, o cativeiro e a vida na Babilônia", "História"),
    ("Retorno e Reconstrução", "Esdras, Neemias, o retorno do exílio e a reconstrução do templo", "História"),
    ("Profecias Mesiânicas", "As promessas de um Salvador, profecias de Isaías, Jeremias e Ezequiel", "Profecia"),
    ("Evangelhos e Vida de Jesus", "Nascimento, ministério, ensinos, milagres, paixão e ressurreição de Cristo", "Jesus Cristo"),
    ("Parábolas de Jesus", "O semeador, o filho pródigo, o bom samaritano e outras parábolas", "Jesus Cristo"),
    ("Milagres de Jesus", "Cura de enfermos, multiplicação dos pães, caminhar sobre as águas", "Jesus Cristo"),
    ("Atos dos Apóstolos", "A descida do Espírito Santo, a expansão da igreja primitiva, Paulo e Pedro", "Igreja Primitiva"),
    ("Epístolas Paulinas", "Romanos, Coríntios, Gálatas, Efésios: doutrina, graça e vida cristã", "Doutrina"),
    ("Epístolas Gerais", "Tiago, Pedro, João, Judas: fé, obras, amor e advertências", "Doutrina"),
    ("Apocalipse", "Revelação de João, profecias escatológicas, o retorno de Cristo", "Profecia"),
    ("Graça e Salvação", "A obra redentora de Cristo, justificação pela fé, o dom da graça", "Doutrina"),
    ("O Espírito Santo", "Pentecostes, dons espirituais, fruto do Espírito, guia e consolador", "Doutrina"),
    ("Oração e Vida Espiritual", "Modelo da oração, intercessão, jejum, comunhão com Deus", "Vida Cristã"),
    ("Fé e Obras", "A relação entre fé e ações, a fé que produz frutos", "Vida Cristã"),
    ("Amor e Perdão", "O mandamento do amor, o amor ao próximo, o perdão em Cristo", "Vida Cristã"),
    ("Tentação e Pecado", "Como resistir à tentação, o poder do pecado e a vitória em Cristo", "Vida Cristã"),
    ("Família e Relacionamentos", "O casamento bíblico, criação dos filhos, relacionamentos saudáveis", "Vida Cristã"),
    ("Ética e Moral Cristã", "Princípios morais, justiça social, honestidade e integridade", "Vida Cristã"),
    ("Angelologia", "Anjos, arcangels, serafins, querubins e sua função na história bíblica", "Anjos e Espíritos"),
    ("Demonologia e Espiritualidade", "Satanás, demônios, espiritualidade, guerra espiritual", "Anjos e Espíritos"),
    ("Escatologia", "Fim dos tempos, o Julgamento Final, o novo céu e a nova terra", "Profecia"),
    ("Cristologia", "A natureza de Cristo, divino e humano, o Verbo encarnado", "Jesus Cristo"),
    ("Eclesiologia", "A igreja como corpo de Cristo, sua missão, estrutura e propósito", "Igreja Primitiva"),
    ("Soteriologia", "A doutrina da salvação: eleição, predestinação, arrependimento, fé", "Doutrina"),
    ("Hamartiologia", "A doutrina do pecado: origem, natureza, consequências e redenção", "Doutrina"),
    ("Pneumatologia", "A doutrina do Espírito Santo: natureza, obras e dons", "Doutrina"),
    ("Bibliologia", "A doutrina das Escrituras: inspiração, autoridade, canonicidade", "Fundamentos"),
    ("Teologia Prática", "Aplicação da fé no dia a dia, discipulado, evangelismo e serviço", "Vida Cristã"),
    ("Profetas do Antigo Testamento", "Isaías, Jeremias, Ezequiel, Daniel, Oséias e os 12 menores", "Profecia"),
    ("Livros Poéticos", "Salmos, Provérbios, Jó, Eclesiastes, Cantares: sabedoria e adoração", "Fundamentos"),
    ("Livros de Sabedoria", "Provérbios, Eclesiastes, Jó: o conhecimento prático de Deus", "Fundamentos"),
    ("História de Israel", "Da criação ao exílio: os grandes eventos da história de Israel", "História"),
    ("A Nova Aliança", "A aliança em Cristo, a nova relação entre Deus e a humanidade", "Doutrina"),
    ("Dons Espirituais", "Profecia, cura, línguas, interpretação: o funcionamento dos dons", "Anjos e Espíritos"),
    ("Batismo e Ceia", "Os sacramentos/ordenanças: significado, modo e importância", "Vida Cristã"),
    ("Discipulado", "O chamado a seguir a Cristo, o crescimento na fé, multiplicação", "Vida Cristã"),
    ("Evangelismo e Missões", "A Grande Comissão, testemunho, proclamação do evangelho", "Vida Cristã"),
    ("Justiça Social", "A preocupação de Deus com os pobres, órfãos, viúvas e oprimidos", "Vida Cristã"),
    ("Criação e Meio Ambiente", "O mandato do creation care, responsabilidade com a criação de Deus", "Fundamentos"),
    ("Sofrimento e Perseverança", "O propósito do sofrimento, a perseverança da fé, esperança na dor", "Vida Cristã"),
    ("A Promessa da Terra Prometida", "A aliança abraâmica, Canaã como tipo e realidade", "Fundamentos"),
    ("Daniel e Profecias", "As 4 bestas, a pedra sem mãos, a 70ª semana, ressurreição e julgamento", "Profecia"),
    ("Ressurreição e Vida Eterna", "A ressurreição de Cristo, a ressurreição dos mortos, a vida eterna", "Profecia"),
    ("Segurança da Salvação", "Segurança do crente, selo do Espírito, perseverança dos santos", "Doutrina"),
    ("O Reino de Deus", "O conceito de Reino, presente e futuro, invadeção do Reino", "Jesus Cristo"),
    ("Miséria e Misericórdia", "A miséria humana e a misericórdia divina: o paradoxo da queda e da graça", "Doutrina"),
    ("Predestinação e Livre Arbítrio", "O debate teológico: soberania divina e responsabilidade humana", "Doutrina"),
    ("Cultura e Sociedade Cristã", "Engajamento cultural, influência da igreja na sociedade", "Vida Cristã"),
]

def populate():
    if not os.path.exists(DB_PATH):
        print(f"ERRO: Banco nao encontrado: {DB_PATH}")
        return False

    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()

    c.execute("SELECT COUNT(*) FROM topics")
    count = c.fetchone()[0]
    if count > 0:
        print(f"Banco ja populado com {count} topics. Limpando...")
        c.execute("DELETE FROM topics")

    seen = set()
    for name, desc, category in TOPICS:
        if name not in seen:
            seen.add(name)
            c.execute("INSERT OR IGNORE INTO topics (name, description, category) VALUES (?, ?, ?)",
                      (name, desc, category))

    conn.commit()

    c.execute("SELECT COUNT(*) FROM topics")
    final_count = c.fetchone()[0]
    print(f"Populado com {final_count} topics em {DB_PATH}")

    conn.close()
    return True

if __name__ == "__main__":
    populate()
