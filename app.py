from flask import Flask, render_template, request, jsonify
import os
from dotenv import load_dotenv
import openai
from datetime import datetime, timedelta
import requests
from geopy.geocoders import Nominatim
import pytz

# 加载环境变量
load_dotenv()

app = Flask(__name__)
openai.api_key = os.getenv('OPENAI_API_KEY')

def get_weather_forecast(city):
    """获取城市天气预报"""
    api_key = os.getenv('WEATHER_API_KEY')
    base_url = "http://api.openweathermap.org/data/2.5/forecast"
    params = {
        'q': city,
        'appid': api_key,
        'units': 'metric',
        'lang': 'zh_cn'
    }
    response = requests.get(base_url, params=params)
    return response.json()

def get_location_info(city):
    """获取城市地理信息"""
    geolocator = Nominatim(user_agent="travel_planner")
    location = geolocator.geocode(city)
    return location

def generate_travel_plan(destination, duration, preferences):
    """使用OpenAI生成旅行计划"""
    prompt = f"""
    请为以下旅行需求制定详细的旅行计划：
    目的地：{destination}
    旅行天数：{duration}
    特殊偏好：{preferences}
    
    请包含以下信息：
    1. 每日行程安排
    2. 推荐景点及开放时间
    3. 交通建议
    4. 当地美食推荐
    5. 注意事项
    """
    
    response = openai.ChatCompletion.create(
        model="gpt-3.5-turbo",
        messages=[
            {"role": "system", "content": "你是一个专业的旅行规划师，擅长制定详细的旅行计划。"},
            {"role": "user", "content": prompt}
        ]
    )
    return response.choices[0].message.content

@app.route('/')
def home():
    return render_template('index.html')

@app.route('/generate_plan', methods=['POST'])
def generate_plan():
    data = request.json
    destination = data.get('destination')
    duration = data.get('duration')
    preferences = data.get('preferences')
    
    # 获取天气信息
    weather_info = get_weather_forecast(destination)
    
    # 获取位置信息
    location_info = get_location_info(destination)
    
    # 生成旅行计划
    travel_plan = generate_travel_plan(destination, duration, preferences)
    
    response = {
        'travel_plan': travel_plan,
        'weather_info': weather_info,
        'location_info': {
            'latitude': location_info.latitude if location_info else None,
            'longitude': location_info.longitude if location_info else None
        }
    }
    
    return jsonify(response)

if __name__ == '__main__':
    app.run(debug=True) 