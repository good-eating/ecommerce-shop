import PyPDF2, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

reader = PyPDF2.PdfReader(r'D:/study/junior2/shop/《网络应用开发》课程设计.pdf')
for i, page in enumerate(reader.pages):
    print(f'--- 第{i+1}页 ---')
    text = page.extract_text()
    print(text)
