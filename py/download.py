# coding=utf-8

import os
from flask import Flask, send_from_directory, request

app = Flask(__name__)


@app.route("/download/<path:filename>", methods=['POST', 'GET'])
def downloader(filename):
    """
    不支持断点的下载
    """
    data = request.values.get('key')
    print data
    dirpath = 'D:/test'
    return send_from_directory(dirpath, filename, as_attachment=True)  # as_attachment=True 一定要写，不然会变成打开，而不是下载


if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=True)  # 需要关闭防火墙
