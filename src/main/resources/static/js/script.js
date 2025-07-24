
        // Smooth scrolling for navigation links
        document.querySelectorAll('a[href^="#"]').forEach(anchor => {
            anchor.addEventListener('click', function (e) {
                e.preventDefault();
                const target = document.querySelector(this.getAttribute('href'));
                if (target) {
                    target.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start'
                    });
                }
            });
        });

        // Mobile menu toggle
        const mobileMenuButton = document.querySelector('.md\\:hidden button');
        const mobileMenu = document.createElement('div');
        mobileMenu.className = 'md:hidden bg-white shadow-lg rounded-lg mt-2 p-4 space-y-4 hidden';
        mobileMenu.innerHTML = `
            <a href="#features" class="block text-gray-700 hover:text-purple-600 transition-colors">Tính năng</a>
            <a href="#pricing" class="block text-gray-700 hover:text-purple-600 transition-colors">Giá cả</a>
            <a href="#contact" class="block text-gray-700 hover:text-purple-600 transition-colors">Liên hệ</a>
            <button class="w-full bg-purple-600 text-white px-6 py-2 rounded-full font-semibold hover:bg-purple-700 transition-colors">
                Đăng nhập
            </button>
        `;
        
        mobileMenuButton.parentNode.appendChild(mobileMenu);
        
        mobileMenuButton.addEventListener('click', function() {
            mobileMenu.classList.toggle('hidden');
        });

        // Navbar background on scroll
        window.addEventListener('scroll', function() {
            const navbar = document.querySelector('nav');
            if (window.scrollY > 50) {
                navbar.style.background = 'rgba(102, 126, 234, 0.95)';
                navbar.style.backdropFilter = 'blur(20px)';
            } else {
                navbar.style.background = 'rgba(255, 255, 255, 0.1)';
                navbar.style.backdropFilter = 'blur(10px)';
            }
        });

        // Counter animation for stats
        function animateCounters() {
            const counters = document.querySelectorAll('.text-4xl.font-bold');
            
            counters.forEach(counter => {
                const target = counter.textContent;
                const numericValue = parseInt(target.replace(/[^\d]/g, ''));
                const suffix = target.replace(/[\d]/g, '');
                
                if (numericValue) {
                    let current = 0;
                    const increment = numericValue / 100;
                    const timer = setInterval(() => {
                        current += increment;
                        if (current >= numericValue) {
                            counter.textContent = target;
                            clearInterval(timer);
                        } else {
                            counter.textContent = Math.floor(current) + suffix;
                        }
                    }, 20);
                }
            });
        }

        // Intersection Observer for animations
        const observerOptions = {
            threshold: 0.1,
            rootMargin: '0px 0px -50px 0px'
        };

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.style.opacity = '1';
                    entry.target.style.transform = 'translateY(0)';
                    
                    // Animate counters when stats section is visible
                    if (entry.target.closest('.gradient-bg') && entry.target.querySelector('.text-4xl')) {
                        animateCounters();
                    }
                }
            });
        }, observerOptions);

        // Observe elements for animation
        document.querySelectorAll('.hover-scale, .grid > div').forEach(el => {
            el.style.opacity = '0';
            el.style.transform = 'translateY(30px)';
            el.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
            observer.observe(el);
        });

        // Parallax effect for hero section
        window.addEventListener('scroll', function() {
            const scrolled = window.pageYOffset;
            const parallaxElements = document.querySelectorAll('.float-animation');
            
            parallaxElements.forEach((element, index) => {
                const speed = 0.5 + (index * 0.1);
                element.style.transform = `translateY(${scrolled * speed}px) translateY(-20px)`;
            });
        });

        // Form validation and interaction
        document.querySelectorAll('button').forEach(button => {
            button.addEventListener('click', function(e) {
                if (this.textContent.includes('Bắt đầu miễn phí') || 
                    this.textContent.includes('Dùng thử miễn phí')) {
                    e.preventDefault();
                    
                    // Create modal for sign up
                    const modal = document.createElement('div');
                    modal.className = 'fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4';
                    modal.innerHTML = `
                        <div class="bg-white rounded-2xl p-8 max-w-md w-full relative">
                            <button class="absolute top-4 right-4 text-gray-400 hover:text-gray-600 text-xl">
                                <i class="fas fa-times"></i>
                            </button>
                            <div class="text-center mb-6">
                                <div class="w-16 h-16 bg-gradient-to-r from-purple-500 to-indigo-600 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <i class="fas fa-rocket text-white text-2xl"></i>
                                </div>
                                <h3 class="text-2xl font-bold text-gray-900 mb-2">Bắt đầu miễn phí</h3>
                                <p class="text-gray-600">Tạo tài khoản và khám phá FileHub ngay hôm nay</p>
                            </div>
                            
                            <form class="space-y-4">
                                <div>
                                    <input type="text" placeholder="Họ và tên" 
                                           class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none">
                                </div>
                                <div>
                                    <input type="email" placeholder="Email" 
                                           class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none">
                                </div>
                                <div>
                                    <input type="password" placeholder="Mật khẩu" 
                                           class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent outline-none">
                                </div>
                                <button type="submit" class="w-full bg-purple-600 text-white py-3 rounded-lg font-semibold hover:bg-purple-700 transition-colors">
                                    Tạo tài khoản miễn phí
                                </button>
                            </form>
                            
                            <div class="mt-6 text-center">
                                <p class="text-sm text-gray-600">
                                    Đã có tài khoản? 
                                    <a href="#" class="text-purple-600 hover:text-purple-700 font-semibold">Đăng nhập</a>
                                </p>
                            </div>
                        </div>
                    `;
                    
                    document.body.appendChild(modal);
                    
                    // Close modal functionality
                    modal.querySelector('button').addEventListener('click', () => {
                        document.body.removeChild(modal);
                    });
                    
                    modal.addEventListener('click', (e) => {
                        if (e.target === modal) {
                            document.body.removeChild(modal);
                        }
                    });
                }
            });
        });

        // Add typing effect to hero title
        // function typeEffect() {
        //     const heroTitle = document.querySelector('h1');
        //     const text = heroTitle.innerHTML;
        //     heroTitle.innerHTML = '';
            
        //     let i = 0;
        //     const timer = setInterval(() => {
        //         if (i < text.length) {
        //             heroTitle.innerHTML += text.charAt(i);
        //             i++;
        //         } else {
        //             clearInterval(timer);
        //         }
        //     }, 50);
        // }

        // // Initialize animations when page loads
        // window.addEventListener('load', function() {
        //     setTimeout(typeEffect, 500);
        // });

        // Add tooltip functionality
        document.querySelectorAll('[title]').forEach(element => {
            element.addEventListener('mouseenter', function(e) {
                const tooltip = document.createElement('div');
                tooltip.className = 'absolute bg-gray-900 text-white px-2 py-1 rounded text-sm z-50';
                tooltip.textContent = this.getAttribute('title');
                tooltip.style.top = (e.pageY - 30) + 'px';
                tooltip.style.left = e.pageX + 'px';
                document.body.appendChild(tooltip);
                
                this.setAttribute('data-tooltip', 'true');
                this.removeAttribute('title');
            });
            
            element.addEventListener('mouseleave', function() {
                const tooltip = document.querySelector('.absolute.bg-gray-900');
                if (tooltip) {
                    document.body.removeChild(tooltip);
                }
            });
        });
