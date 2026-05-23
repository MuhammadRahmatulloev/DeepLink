import os
import fitz
import docx
import openpyxl
import pytesseract
from PIL import Image


def extract_text_from_pdf(file_path: str) -> str:
    doc = fitz.open(file_path)
    text = ''
    for page in doc:
        text += page.get_text()
    doc.close()
    return text.strip()


def extract_text_from_docx(file_path: str) -> str:
    doc = docx.Document(file_path)
    return '\n'.join([para.text for para in doc.paragraphs if para.text.strip()])


def extract_text_from_xlsx(file_path: str) -> str:
    wb = openpyxl.load_workbook(file_path, data_only=True)
    lines = []
    for sheet in wb.worksheets:
        lines.append(f'Sheet: {sheet.title}')
        for row in sheet.iter_rows(values_only=True):
            row_text = ' | '.join([str(cell) for cell in row if cell is not None])
            if row_text.strip():
                lines.append(row_text)
    return '\n'.join(lines)


def extract_text_from_image(file_path: str) -> str:
    image = Image.open(file_path)
    return pytesseract.image_to_string(image).strip()


def extract_text(file_path: str, file_extension: str) -> str:
    ext = file_extension.lower()

    if ext == '.pdf':
        return extract_text_from_pdf(file_path)
    elif ext in ['.docx', '.doc']:
        return extract_text_from_docx(file_path)
    elif ext in ['.xlsx', '.xls']:
        return extract_text_from_xlsx(file_path)
    elif ext in ['.jpg', '.jpeg', '.png', '.bmp', '.tiff']:
        return extract_text_from_image(file_path)
    else:
        raise ValueError(f'Unsupported file type: {ext}')


ALLOWED_EXTENSIONS = ['.pdf', '.docx', '.doc', '.xlsx', '.xls', '.jpg', '.jpeg', '.png', '.bmp', '.tiff']
MAX_FILE_SIZE_MB = 20